package com.marketplace.product;

import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

        private final ProductLikeService productLikeService;
        private final UserService userService;

        /**
         * Toggle like - called from Ajax
         * Returns simple text response for JavaScript to check
         */
        @PostMapping("/toggle/{productId}")
        @ResponseBody
        public String toggleLike(@PathVariable Long productId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                if (userDetails == null) {
                        return "error:not_logged_in";
                }

                try {
                        User user = userService.findByUsername(userDetails.getUsername())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        return productLikeService.toggleLike(user, productId);
                } catch (Exception e) {
                        e.printStackTrace();
                        return "error:" + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                }
        }
}
