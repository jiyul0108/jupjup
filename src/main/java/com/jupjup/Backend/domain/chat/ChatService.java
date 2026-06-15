package com.jupjup.Backend.domain.chat;

import com.jupjup.Backend.domain.chat.dto.ChatMessageRequest;
import com.jupjup.Backend.domain.chat.dto.ChatMessageResponse;
import com.jupjup.Backend.domain.chat.dto.ChatRoomCreateRequest;
import com.jupjup.Backend.domain.chat.dto.ChatRoomResponse;
import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request, String email) {
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getId().equals(buyer.getId())) {
            throw new BusinessException(ErrorCode.CHAT_SELF_NOT_ALLOWED);
        }

        return chatRoomRepository
                .findByProductIdAndBuyerId(product.getId(), buyer.getId())
                .map(ChatRoomResponse::new)
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .product(product)
                            .buyer(buyer)
                            .seller(product.getSeller())
                            .build();
                    return new ChatRoomResponse(chatRoomRepository.save(room));
                });
    }

    public List<ChatRoomResponse> getMyRooms(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return chatRoomRepository.findAllByUserId(user.getId())
                .stream()
                .map(room -> {
                    int unread = chatMessageRepository.findUnreadMessages(room.getId(), user.getId()).size();
                    return new ChatRoomResponse(room, unread);
                })
                .toList();
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(Long roomId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getBuyer().getId().equals(user.getId()) &&
                !room.getSeller().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        // 입장 시 안읽은 메시지 읽음 처리
        List<ChatMessage> unread = chatMessageRepository.findUnreadMessages(roomId, user.getId());
        unread.forEach(ChatMessage::markAsRead);
        chatMessageRepository.saveAll(unread);

        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::new)
                .toList();
    }

    public long getTotalUnread(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return chatMessageRepository.countTotalUnread(user.getId());
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, ChatMessageRequest request, String email) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getBuyer().getId().equals(sender.getId()) &&
                !room.getSeller().getId().equals(sender.getId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .build();

        return new ChatMessageResponse(chatMessageRepository.save(message));
    }
}