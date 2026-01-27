package com.marketplace.chat.dto;

import com.marketplace.chat.model.ChatRoom;
import com.marketplace.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productImage;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;

    public static ChatRoomDTO fromEntity(ChatRoom chatRoom, Long currentUserId) {
        User otherUser = chatRoom.getOtherParticipant(currentUserId);

        // Get first product image if available
        String productImage = null;
        if (chatRoom.getProduct().getImages() != null && !chatRoom.getProduct().getImages().isEmpty()) {
            productImage = chatRoom.getProduct().getImages().get(0).getImageUrl();
        }

        // Determine unread count based on current user
        Integer unreadCount = chatRoom.getBuyer().getId().equals(currentUserId)
                ? chatRoom.getUnreadCountBuyer()
                : chatRoom.getUnreadCountSeller();

        return ChatRoomDTO.builder()
                .id(chatRoom.getId())
                .productId(chatRoom.getProduct().getId())
                .productTitle(chatRoom.getProduct().getTitle())
                .productImage(productImage)
                .otherUserId(otherUser.getId())
                .otherUserName(otherUser.getUsername())
                .otherUserAvatar(otherUser.getAvatarUrl())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .unreadCount(unreadCount)
                .build();
    }
}
