package com.jupjup.Backend.domain.chat;

import com.jupjup.Backend.domain.chat.dto.ChatMessageResponse;
import com.jupjup.Backend.domain.chat.dto.ChatRoomCreateRequest;
import com.jupjup.Backend.domain.chat.dto.ChatRoomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 생성 (이미 존재하면 기존 방 반환)
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.createRoom(request, email));
    }

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.getMyRooms(email));
    }

    // 채팅방 이전 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.getMessages(roomId, email));
    }

    // 전체 안읽은 메시지 수
    @GetMapping("/unread")
    public ResponseEntity<Long> getTotalUnread(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.getTotalUnread(email));
    }
}