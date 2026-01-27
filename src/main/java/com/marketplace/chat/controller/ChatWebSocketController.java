package com.marketplace.chat.controller;

import com.marketplace.chat.dto.ChatMessageDTO;
import com.marketplace.chat.model.ChatMessage;
import com.marketplace.chat.service.ChatMessageService;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Receive message from client and broadcast to chat room participants
     * Client sends to: /app/chat.send/{chatRoomId}
     * Server broadcasts to: /topic/chatroom/{chatRoomId}
     */
    @MessageMapping("/chat.send/{chatRoomId}")
    public void sendMessage(@DestinationVariable Long chatRoomId,
            @Payload ChatMessageDTO messageDTO,
            Principal principal) {
        try {
            // Get current user from principal
            Long senderId = getUserIdFromPrincipal(principal);

            // Save message
            ChatMessage savedMessage = chatMessageService.saveMessage(
                    chatRoomId,
                    senderId,
                    messageDTO.getContent());

            // Convert to DTO
            ChatMessageDTO response = ChatMessageDTO.fromEntity(savedMessage);

            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + chatRoomId,
                    response);

        } catch (Exception e) {
            // Log error and send error message
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();

            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + chatRoomId + "/errors",
                    "Failed to send message: " + e.getMessage());
        }
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            Object principalObj = auth.getPrincipal();

            if (principalObj instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User userDetails = (org.springframework.security.core.userdetails.User) principalObj;
                User user = userService.findByUsername(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                return user.getId();
            }
        }

        // Fallback: try to parse username as ID or lookup by username
        String username = principal.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}
