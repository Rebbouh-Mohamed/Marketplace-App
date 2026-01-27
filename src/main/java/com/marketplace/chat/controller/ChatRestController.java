package com.marketplace.chat.controller;

import com.marketplace.chat.dto.ChatMessageDTO;
import com.marketplace.chat.dto.ChatRoomDTO;
import com.marketplace.chat.dto.SendMessageRequest;
import com.marketplace.chat.dto.StartChatRequest;
import com.marketplace.chat.model.ChatMessage;
import com.marketplace.chat.model.ChatRoom;
import com.marketplace.chat.service.ChatMessageService;
import com.marketplace.chat.service.ChatRoomService;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Get all chat rooms for current user
     * GET /api/chat/rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getUserChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatRoom> chatRooms = chatRoomService.getUserChatRooms(currentUser.getId());
        List<ChatRoomDTO> dtos = chatRooms.stream()
                .map(room -> ChatRoomDTO.fromEntity(room, currentUser.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Start chat with seller about a product
     * POST /api/chat/start
     */
    @PostMapping("/start")
    public ResponseEntity<ChatRoomDTO> startChat(@RequestBody StartChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(request.getProductId(), currentUser.getId());
        ChatRoomDTO dto = ChatRoomDTO.fromEntity(chatRoom, currentUser.getId());

        return ResponseEntity.ok(dto);
    }

    /**
     * Get messages for a chat room
     * GET /api/chat/room/{chatRoomId}/messages
     */
    @GetMapping("/room/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(@PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatMessage> messages = chatMessageService.getChatRoomMessages(chatRoomId, currentUser.getId());
        List<ChatMessageDTO> dtos = messages.stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Send message (also broadcasts via WebSocket)
     * POST /api/chat/room/{chatRoomId}/send
     */
    @PostMapping("/room/{chatRoomId}/send")
    public ResponseEntity<ChatMessageDTO> sendMessage(@PathVariable Long chatRoomId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatMessage message = chatMessageService.saveMessage(
                chatRoomId,
                currentUser.getId(),
                request.getContent());

        ChatMessageDTO dto = ChatMessageDTO.fromEntity(message);

        // Broadcast via WebSocket
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, dto);

        return ResponseEntity.ok(dto);
    }

    /**
     * Mark messages as read
     * POST /api/chat/room/{chatRoomId}/read
     */
    @PostMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        chatMessageService.markMessagesAsRead(chatRoomId, currentUser.getId());

        return ResponseEntity.ok().build();
    }
}
