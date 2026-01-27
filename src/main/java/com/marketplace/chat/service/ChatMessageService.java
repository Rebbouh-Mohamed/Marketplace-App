package com.marketplace.chat.service;

import com.marketplace.chat.model.ChatMessage;
import com.marketplace.chat.model.ChatRoom;
import com.marketplace.chat.repository.ChatMessageRepository;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    /**
     * Save a new message
     */
    public ChatMessage saveMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId, senderId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update chat room's last message
        chatRoomService.updateLastMessage(chatRoomId, content, senderId);

        return savedMessage;
    }

    /**
     * Get all messages for a chat room
     */
    public List<ChatMessage> getChatRoomMessages(Long chatRoomId, Long userId) {
        // Validate user is participant
        chatRoomService.getChatRoom(chatRoomId, userId);

        return chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }

    /**
     * Mark all messages in a chat room as read for the current user
     */
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        chatMessageRepository.markAllAsRead(chatRoomId, userId);
        chatRoomService.resetUnreadCount(chatRoomId, userId);
    }
}
