package com.codewish.controller;

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
import java.util.Optional;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

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

        model.addAttribute("user", user);
        model.addAttribute("group", groupOpt.get());
        return "create-expense";
    }

    @PostMapping("/create")
    public String createExpense(@RequestParam Long groupId, @RequestParam String description,
                                @RequestParam BigDecimal amount, @RequestParam String expenseDate,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        LocalDate date = LocalDate.parse(expenseDate);
        Expense expense = expenseService.createExpenseWithEqualSplit(groupId, description, amount, user.getId(), date);

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
        String name = userService.findById(expense.getPaidByUserId()).get().getUsername();
        model.addAttribute("user", user);
        model.addAttribute("expense", expense);
        model.addAttribute("name", name);
        model.addAttribute("splits", expenseService.getExpenseSplits(id));

        return "expense-details";
    }
}
