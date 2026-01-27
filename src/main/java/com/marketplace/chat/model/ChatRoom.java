package com.marketplace.chat.model;

import com.marketplace.product.Product;
import com.marketplace.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "buyer_id",
        "seller_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "unread_count_buyer")
    @Builder.Default
    private Integer unreadCountBuyer = 0;

    @Column(name = "unread_count_seller")
    @Builder.Default
    private Integer unreadCountSeller = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if user is a participant in this chat room
     */
    public boolean isParticipant(Long userId) {
        return buyer.getId().equals(userId) || seller.getId().equals(userId);
    }

    /**
     * Get the other participant in the chat (not the current user)
     */
    public User getOtherParticipant(Long currentUserId) {
        return buyer.getId().equals(currentUserId) ? seller : buyer;
    }
}
