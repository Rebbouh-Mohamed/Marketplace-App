package com.marketplace.repository;

import com.marketplace.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    // Get all messages for a product, sorted by time
    List<ChatMessage> findByProductIdOrderByTimestampAsc(Long productId);

    // Get messages after a specific ID (for polling new messages)
    List<ChatMessage> findByProductIdAndIdGreaterThanOrderByTimestampAsc(
            Long productId, Long afterId);

    // Get all messages where user is either sender or receiver
    List<ChatMessage> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId);
}
