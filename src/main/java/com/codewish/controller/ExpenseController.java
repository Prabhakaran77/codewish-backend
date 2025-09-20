package com.codewish.controller;

import com.codewish.model.GroupMember;
import com.codewish.model.User;
import com.codewish.model.Group;
import com.codewish.model.Expense;
import com.codewish.service.GroupService;
import com.codewish.service.ExpenseService;
import com.codewish.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private GroupService groupService;

    @GetMapping("/create")
    public String createExpensePage(@RequestParam Long groupId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Group> groupOpt = groupService.findById(groupId);
        if (!groupOpt.isPresent()) {
            return "redirect:/dashboard";
        }

        List<GroupMember> members = groupService.getGroupMembers(groupId);

        model.addAttribute("user", user);
        model.addAttribute("group", groupOpt.get());
        model.addAttribute("members", members);
        return "create-expense";
    }

    @PostMapping("/create")
    public String createExpense(@RequestParam Long groupId, @RequestParam String description,
                                @RequestParam BigDecimal amount, @RequestParam String expenseDate,
                                @RequestParam Long paidByUserId,
                                @RequestParam(required = false) List<Long> participantIds,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        LocalDate date = LocalDate.parse(expenseDate);

        Expense expense;
        if (participantIds != null && !participantIds.isEmpty()) {
            expense = expenseService.createExpenseWithCustomSplit(groupId, description, amount, paidByUserId, date, participantIds);
        } else {
            expense = expenseService.createExpenseWithEqualSplit(groupId, description, amount, paidByUserId, date);
        }

        redirectAttributes.addFlashAttribute("success", "Expense added successfully!");
        return "redirect:/groups/" + groupId;
    }

    @GetMapping("/{id}")
    public String viewExpense(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Expense> expenseOpt = expenseService.findById(id);
        if (!expenseOpt.isPresent()) {
            return "redirect:/dashboard";
        }

        Expense expense = expenseOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("expense", expense);
        model.addAttribute("splits", expenseService.getExpenseSplits(id));

        return "expense-details";
    }
}