package com.codewish.controller;

import com.codewish.model.User;
import com.codewish.model.Group;
import com.codewish.model.GroupMember;
import com.codewish.model.Expense;
import com.codewish.service.GroupService;
import com.codewish.service.ExpenseService;
import com.codewish.service.UserService;
import com.codewish.service.BalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    @Autowired
    private BalanceService balanceService;

    @GetMapping("/create")
    public String createGroupPage(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "create-group";
    }

    @PostMapping("/create")
    public String createGroup(@RequestParam String name, @RequestParam String description,
                              HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Group group = groupService.createGroup(name, description, user.getId());
        redirectAttributes.addFlashAttribute("success", "Group created successfully!");
        return "redirect:/groups/" + group.getId();
    }

    @GetMapping("/{id}")
    public String viewGroup(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Group> groupOpt = groupService.findById(id);
        if (!groupOpt.isPresent()) {
            return "redirect:/dashboard";
        }

        Group group = groupOpt.get();
        List<GroupMember> members = groupService.getGroupMembers(id);
        List<Expense> expenses = expenseService.getGroupExpenses(id);

        // Calculate user's balance in this group
        BigDecimal userBalance = balanceService.getUserBalanceInGroup(id, user.getId());

        model.addAttribute("user", user);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        model.addAttribute("expenses", expenses);
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("userService", userService);

        return "group-details";
    }

    @GetMapping("/{id}/settlements")
    public String viewGroupSettlements(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Group> groupOpt = groupService.findById(id);
        if (!groupOpt.isPresent()) {
            return "redirect:/dashboard";
        }

        Group group = groupOpt.get();
        List<BalanceService.Settlement> settlements = balanceService.getGroupSettlements(id);

        boolean isGroupAdmin = group.getCreatedBy().equals(user.getId());


        model.addAttribute("user", user);
        model.addAttribute("group", group);
        model.addAttribute("settlements", settlements);
        model.addAttribute("isGroupAdmin", isGroupAdmin);


        return "group-settlements";
    }

    @PostMapping("/{groupId}/settle")
    public String markAsSettled(@PathVariable Long groupId,
                                @RequestParam Long fromUserId,
                                @RequestParam Long toUserId,
                                @RequestParam BigDecimal amount,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            expenseService.createSettlementExpense(groupId, fromUserId, toUserId, amount);
            redirectAttributes.addFlashAttribute("success", "Settlement recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to record settlement: " + e.getMessage());
        }

        return "redirect:/groups/" + groupId + "/settlements";
    }

    @PostMapping("/{id}/add-member")
    public String addMember(@PathVariable Long id, @RequestParam String username,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<User> memberToAdd = userService.findByUsername(username);
        if (!memberToAdd.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/groups/" + id;
        }

        boolean added = groupService.addUserToGroup(id, memberToAdd.get().getId());
        if (added) {
            redirectAttributes.addFlashAttribute("success", "Member added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "User is already in the group");
        }

        return "redirect:/groups/" + id;
    }
}