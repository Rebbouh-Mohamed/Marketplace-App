package com.marketplace.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Long senderId;
    private Long receiverId;
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;
}
