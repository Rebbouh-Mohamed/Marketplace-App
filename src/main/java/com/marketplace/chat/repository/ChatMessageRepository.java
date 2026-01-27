package com.marketplace.chat.repository;

import com.marketplace.chat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Get all messages for a chat room, ordered by time (oldest first)
     */
    List<ChatMessage> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);

    /**
     * Get recent messages with limit (newest first)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findRecentMessages(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * Count unread messages for a user in a chat room
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.sender.id != :userId AND cm.isRead = false")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    /**
     * Mark all messages as read for the current user (bulk update)
     */
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true WHERE cm.chatRoom.id = :chatRoomId AND cm.sender.id != :userId")
    void markAllAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
