package com.marketplace.chat.service;

import com.marketplace.chat.model.ChatRoom;
import com.marketplace.chat.repository.ChatRoomRepository;
import com.marketplace.product.Product;
import com.marketplace.product.ProductRepository;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get or create a chat room between buyer and seller for a specific product
     */
    public ChatRoom getOrCreateChatRoom(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Long sellerId = product.getSeller().getId();

        // Don't allow seller to chat with themselves
        if (sellerId.equals(buyerId)) {
            throw new IllegalArgumentException("Cannot create chat room with yourself");
        }

        // Check if chat room already exists
        return chatRoomRepository
                .findByProductIdAndBuyerIdAndSellerId(productId, buyerId, sellerId)
                .orElseGet(() -> createNewChatRoom(product, buyerId, sellerId));
    }

    private ChatRoom createNewChatRoom(Product product, Long buyerId, Long sellerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        ChatRoom chatRoom = ChatRoom.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .unreadCountBuyer(0)
                .unreadCountSeller(0)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    /**
     * Get all chat rooms for a user
     */
    public List<ChatRoom> getUserChatRooms(Long userId) {
        return chatRoomRepository.findByUserId(userId);
    }

    /**
     * Get specific chat room and validate user is participant
     */
    public ChatRoom getChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isParticipant(userId)) {
            throw new AccessDeniedException("You are not a participant in this chat");
        }

        return chatRoom;
    }

    /**
     * Update last message info and increment unread count
     */
    public void updateLastMessage(Long chatRoomId, String message, Long senderId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        chatRoom.setLastMessage(message);
        chatRoom.setLastMessageTime(LocalDateTime.now());

        // Increment unread count for the receiver
        if (chatRoom.getBuyer().getId().equals(senderId)) {
            chatRoom.setUnreadCountSeller(chatRoom.getUnreadCountSeller() + 1);
        } else {
            chatRoom.setUnreadCountBuyer(chatRoom.getUnreadCountBuyer() + 1);
        }

        chatRoomRepository.save(chatRoom);
    }

    /**
     * Reset unread count for user
     */
    public void resetUnreadCount(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getBuyer().getId().equals(userId)) {
            chatRoom.setUnreadCountBuyer(0);
        } else {
            chatRoom.setUnreadCountSeller(0);
        }

        chatRoomRepository.save(chatRoom);
    }
}
