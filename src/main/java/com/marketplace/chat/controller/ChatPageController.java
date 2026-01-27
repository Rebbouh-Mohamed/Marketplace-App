package com.marketplace.chat.controller;

import com.marketplace.chat.model.ChatMessage;
import com.marketplace.chat.model.ChatRoom;
import com.marketplace.chat.service.ChatMessageService;
import com.marketplace.chat.service.ChatRoomService;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatPageController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    /**
     * Show list of all conversations
     * URL: GET /chat
     * Template: chat/list.html
     */
    @GetMapping
    public String chatList(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatRoom> chatRooms = chatRoomService.getUserChatRooms(currentUser.getId());

        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("currentUser", currentUser);

        return "chat/list";
    }

    /**
     * Start or continue chat about a product (from product details page)
     * URL: GET /chat/product/{productId}
     * Redirects to: /chat/room/{chatRoomId}
     */
    @GetMapping("/product/{productId}")
    public String startChatWithSeller(@PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(productId, currentUser.getId());

        return "redirect:/chat/room/" + chatRoom.getId();
    }

    /**
     * Show specific chat room with real-time messaging
     * URL: GET /chat/room/{chatRoomId}
     * Template: chat/room.html
     */
    @GetMapping("/room/{chatRoomId}")
    public String chatRoom(@PathVariable Long chatRoomId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId, currentUser.getId());
        List<ChatMessage> messages = chatMessageService.getChatRoomMessages(chatRoomId, currentUser.getId());

        // Mark messages as read
        chatMessageService.markMessagesAsRead(chatRoomId, currentUser.getId());

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", chatRoom.getOtherParticipant(currentUser.getId()));
        model.addAttribute("product", chatRoom.getProduct());

        return "chat/room";
    }
}
