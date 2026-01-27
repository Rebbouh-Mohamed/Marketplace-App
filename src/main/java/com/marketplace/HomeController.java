package com.marketplace;

import com.marketplace.product.CategoryService;
import com.marketplace.product.ProductService;
import com.marketplace.product.ProductLikeRepository;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductLikeRepository productLikeRepository;
    private final UserService userService;

    @GetMapping("/")
    public String home(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            Model model) {
        // Fetch recent products for the "Fresh Finds" section
        model.addAttribute("recentProducts", productService.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))));

        // Fetch categories for the "Trending" section
        model.addAttribute("categories", categoryService.findAll());

        if (userDetails != null) {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                java.util.Set<Long> likedIds = productLikeRepository.findByUserId(user.getId())
                        .stream().map(pl -> pl.getProduct().getId())
                        .collect(java.util.stream.Collectors.toSet());
                model.addAttribute("likedProductIds", likedIds);
            });
        }

        return "index";
    }
}
