package com.marketplace.admin;

import com.marketplace.product.ProductService;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.countUsers());
        model.addAttribute("productCount", productService.countProducts());
        model.addAttribute("recentUsers", userService.getRecentUsers());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable) {
        model.addAttribute("users", userService.getUsers(search, pageable));
        model.addAttribute("search", search);
        return "admin/users";
    }

    @org.springframework.web.bind.annotation.PostMapping("/users/{id}/status")
    public String updateUserStatus(@org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam String status) {
        System.out.println("AdminAction: Update User ID " + id + " to status " + status);
        userService.updateUserStatus(id, com.marketplace.user.UserStatus.valueOf(status));
        return "redirect:/admin/users";
    }

    @org.springframework.web.bind.annotation.PostMapping("/users/{id}/delete")
    public String deleteUser(@org.springframework.web.bind.annotation.PathVariable Long id) {
        System.out.println("AdminAction: Delete User ID " + id);
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/posts")
    public String listPosts(Model model,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable) {
        model.addAttribute("products", productService.getAdminProducts(status, pageable));
        model.addAttribute("status", status);
        return "admin/posts";
    }

    @org.springframework.web.bind.annotation.PostMapping("/posts/{id}/status")
    public String updatePostStatus(@org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam String status) {
        System.out.println("AdminAction: Update Post ID " + id + " to status " + status);
        productService.updateProductStatus(id, com.marketplace.product.Product.ProductStatus.valueOf(status));
        return "redirect:/admin/posts";
    }
}
