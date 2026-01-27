package com.marketplace.controller;

import com.marketplace.model.ChatMessage;
import com.marketplace.service.MessageService;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import com.marketplace.product.Product;
import com.marketplace.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final ProductService productService;

    /**
     * Show general chat overview (all conversations)
     */
    @GetMapping("/chat")
    @Transactional
    public String showGeneralChatPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("currentUserName", currentUser.getUsername());

        return "chat/index"; // returns chat/index.html
    }

    /**
     * Show chat page (Thymeleaf view) for a specific product
     */
    @GetMapping("/chat/product/{productId}")
    @Transactional
    public String showChatPage(
            @PathVariable Long productId,
            @RequestParam(required = false) Long otherUserId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productService.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Long receiverId = null;
        String receiverName = null;

        if (!currentUser.getId().equals(product.getSeller().getId())) {
            // I am the buyer, chatting with the seller
            User seller = userService.findById(product.getSeller().getId())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            receiverId = seller.getId();
            receiverName = seller.getUsername();
        } else if (otherUserId != null) {
            // I am the seller, chatting with a specific buyer
            User otherUser = userService.findById(otherUserId)
                    .orElseThrow(() -> new RuntimeException("Buyer not found"));
            receiverId = otherUser.getId();
            receiverName = otherUser.getUsername();
        }

        model.addAttribute("productId", productId);
        model.addAttribute("productTitle", product.getTitle());
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("currentUserName", currentUser.getUsername());
        model.addAttribute("receiverId", receiverId);
        model.addAttribute("receiverName", receiverName);

        return "chat"; // returns chat.html
    }

    /**
     * REST API - Get all conversations for current user
     */
    @GetMapping("/api/chat/conversations")
    @ResponseBody
    @Transactional
    public ResponseEntity<List<ConversationDTO>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null)
            return ResponseEntity.status(401).build();

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatMessage> allMessages = messageService.getMessagesForUser(currentUser.getId());

        Map<String, ChatMessage> latestMessages = new HashMap<>(); // productID_otherID -> message
        for (ChatMessage msg : allMessages) {
            if (msg.getSenderId() == null || msg.getReceiverId() == null)
                continue;

            Long otherId = msg.getSenderId().equals(currentUser.getId()) ? msg.getReceiverId() : msg.getSenderId();
            String key = msg.getProductId() + "_" + otherId;
            // Only keep the latest message for each conversation
            latestMessages.compute(key,
                    (k, existingMsg) -> (existingMsg == null || msg.getTimestamp().isAfter(existingMsg.getTimestamp()))
                            ? msg
                            : existingMsg);
        }

        List<ConversationDTO> conversations = latestMessages.values().stream()
                .map(msg -> {
                    Long otherId = msg.getSenderId().equals(currentUser.getId()) ? msg.getReceiverId()
                            : msg.getSenderId();
                    if (otherId == null)
                        return null;

                    User other = userService.findById(otherId).orElse(null);
                    Product product = productService.findById(msg.getProductId()).orElse(null);

                    if (other == null || product == null)
                        return null;

                    return new ConversationDTO(
                            msg.getProductId(),
                            product.getTitle(),
                            otherId,
                            other.getUsername(),
                            msg.getContent(),
                            msg.getTimestamp());
                })
                .filter(Objects::nonNull)
                .sorted((c1, c2) -> c2.getLastMessageAt().compareTo(c1.getLastMessageAt()))
                .toList();

        return ResponseEntity.ok(conversations);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ConversationDTO {
        private Long productId;
        private String productTitle;
        private Long otherUserId;
        private String otherUserName;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
    }

    /**
     * REST API - Send a message
     */
    @PostMapping("/api/chat/send")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody ChatMessage message,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            message.setSenderId(currentUser.getId());
            message.setSenderName(currentUser.getUsername());
        }

        System.out
                .println("📨 Received message: " + message.getContent() + " for receiver: " + message.getReceiverId());

        // Set timestamp
        message.setTimestamp(LocalDateTime.now());

        // Save to database
        ChatMessage savedMessage = messageService.saveMessage(message);

        System.out.println("✅ Message saved with ID: " + savedMessage.getId());

        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", savedMessage);

        return ResponseEntity.ok(response);
    }

    /**
     * REST API - Get messages (called every 3 seconds from frontend)
     */
    @GetMapping("/api/chat/messages/{productId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long productId,
            @RequestParam(required = false) Long afterId) {

        List<ChatMessage> messages;

        if (afterId != null && afterId > 0) {
            // Get only NEW messages after this ID
            messages = messageService.getMessagesAfter(productId, afterId);
            System.out.println("📬 Returning " + messages.size() + " new messages after ID " + afterId);
        } else {
            // Get ALL messages (first load)
            messages = messageService.getAllMessages(productId);
            System.out.println("📬 Returning " + messages.size() + " total messages");
        }

        return ResponseEntity.ok(messages);
    }
}
