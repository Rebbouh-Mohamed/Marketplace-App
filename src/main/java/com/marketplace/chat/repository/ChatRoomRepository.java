package com.marketplace.chat.repository;

import com.marketplace.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find existing chat room for a product between buyer and seller
     */
    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);

    /**
     * Find all chat rooms where user is buyer or seller, ordered by most recent
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.buyer.id = :userId OR cr.seller.id = :userId ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    /**
     * Count unread messages for a user (rooms with unread count > 0)
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE (cr.buyer.id = :userId AND cr.unreadCountBuyer > 0) OR (cr.seller.id = :userId AND cr.unreadCountSeller > 0)")
    Long countUnreadRoomsForUser(@Param("userId") Long userId);
}
