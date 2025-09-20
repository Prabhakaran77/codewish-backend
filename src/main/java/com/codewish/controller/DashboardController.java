package com.codewish.controller;

import com.codewish.model.User;
import com.codewish.model.Group;
import com.codewish.service.GroupService;
import com.codewish.service.BalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private BalanceService balanceService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Group> userGroups = groupService.findGroupsByUserId(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("groups", userGroups);

        return "dashboard";
    }
}