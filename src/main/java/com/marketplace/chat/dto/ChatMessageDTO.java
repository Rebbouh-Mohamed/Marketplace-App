package com.marketplace.chat.dto;

import com.marketplace.chat.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private LocalDateTime timestamp;
    private Boolean isRead;

    public static ChatMessageDTO fromEntity(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .senderAvatar(message.getSender().getAvatarUrl())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .isRead(message.getIsRead())
                .build();
    }
}
