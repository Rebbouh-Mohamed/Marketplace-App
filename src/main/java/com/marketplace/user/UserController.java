package com.marketplace.user;

import com.marketplace.product.ProductRepository;
import com.marketplace.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProductRepository productRepository;
    private final com.marketplace.product.ProductLikeRepository productLikeRepository;
    private final ChatRoomRepository chatRoomRepository;

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        // Fetch products listed by this user
        model.addAttribute("myProducts", productRepository.findBySellerId(user.getId(), PageRequest.of(0, 50)));

        // Fetch liked products
        java.util.List<com.marketplace.product.Product> likedProducts = productLikeRepository.findByUserId(user.getId())
                .stream()
                .map(com.marketplace.product.ProductLike::getProduct)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("likedProducts", likedProducts);

        // Fetch conversations history using new ChatRoom system
        java.util.List<com.marketplace.chat.model.ChatRoom> chatRooms = chatRoomRepository.findByUserId(user.getId());

        // Convert to conversation maps for template
        java.util.List<java.util.Map<String, Object>> conversations = chatRooms.stream()
                .map(room -> {
                    java.util.Map<String, Object> convMap = new java.util.HashMap<>();
                    User otherUser = room.getOtherParticipant(user.getId());
                    convMap.put("otherUser", otherUser);
                    convMap.put("product", room.getProduct());

                    // Create a pseudo-message object for template compatibility
                    java.util.Map<String, Object> lastMsgMap = new java.util.HashMap<>();
                    lastMsgMap.put("content", room.getLastMessage());
                    lastMsgMap.put("timestamp", room.getLastMessageTime());
                    lastMsgMap.put("productId", room.getProduct().getId());

                    convMap.put("lastMessage", lastMsgMap);
                    convMap.put("chatRoomId", room.getId());
                    return convMap;
                })
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("messageHistory", conversations);

        // Pass tab parameter to model if present
        String tab = "listings";
        model.addAttribute("activeTab", tab);

        return "user/profile";
    }

    @org.springframework.web.bind.annotation.PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String username,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String phoneNumber,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String address,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String currentPassword,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String newPassword,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
            userService.updateProfile(user.getId(), username, email, phoneNumber, address, currentPassword,
                    newPassword);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");

            // If username changed, we might need to re-login or warn user, but for
            // simplicity:
            if (!user.getUsername().equals(username)) {
                return "redirect:/logout";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?tab=settings";
    }
}
