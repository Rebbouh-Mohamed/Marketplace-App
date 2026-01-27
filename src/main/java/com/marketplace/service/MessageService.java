package com.marketplace.service;

import com.marketplace.model.ChatMessage;
import com.marketplace.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * Save message to database
     */
    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        return messageRepository.save(message);
    }

    /**
     * Get all messages for a product
     */
    public List<ChatMessage> getAllMessages(Long productId) {
        return messageRepository.findByProductIdOrderByTimestampAsc(productId);
    }

    /**
     * Get messages after a specific ID (for polling)
     */
    public List<ChatMessage> getMessagesAfter(Long productId, Long afterId) {
        return messageRepository.findByProductIdAndIdGreaterThanOrderByTimestampAsc(
                productId, afterId);
    }

    /**
     * Get all messages for a user (as sender or receiver)
     */
    public List<ChatMessage> getMessagesForUser(Long userId) {
        return messageRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
    }
}
