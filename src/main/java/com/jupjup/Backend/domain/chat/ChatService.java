package com.jupjup.Backend.domain.chat;

import com.jupjup.Backend.domain.chat.dto.ChatMessageRequest;
import com.jupjup.Backend.domain.chat.dto.ChatMessageResponse;
import com.jupjup.Backend.domain.chat.dto.ChatRoomCreateRequest;
import com.jupjup.Backend.domain.chat.dto.ChatRoomResponse;
import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
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

    // 채팅방 생성 (이미 존재하면 기존 채팅방 반환)
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request, String email) {
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 판매자 본인과는 채팅 불가
        if (product.getSeller().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("본인 상품에는 채팅을 걸 수 없습니다.");
        }

        // 이미 채팅방이 존재하면 기존 방 반환
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

    // 내 채팅방 목록 조회
    public List<ChatRoomResponse> getMyRooms(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return chatRoomRepository.findAllByUserId(user.getId())
                .stream()
                .map(ChatRoomResponse::new)
                .toList();
    }

    // 채팅방 이전 메시지 조회
    public List<ChatMessageResponse> getMessages(Long roomId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 채팅방 참여자 확인
        if (!room.getBuyer().getId().equals(user.getId()) &&
                !room.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다.");
        }

        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::new)
                .toList();
    }

    // 메시지 저장 (STOMP 핸들러에서 호출)
    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, ChatMessageRequest request, String email) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 채팅방 참여자 확인
        if (!room.getBuyer().getId().equals(sender.getId()) &&
                !room.getSeller().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다.");
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .build();

        return new ChatMessageResponse(chatMessageRepository.save(message));
    }
}