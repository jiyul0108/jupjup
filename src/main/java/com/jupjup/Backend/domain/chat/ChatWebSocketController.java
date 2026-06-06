package com.jupjup.Backend.domain.chat;

import com.jupjup.Backend.domain.chat.dto.ChatMessageRequest;
import com.jupjup.Backend.domain.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // 핸드셰이크 때 저장한 email을 세션 속성에서 꺼냄
        String email = (String) headerAccessor.getSessionAttributes().get("email");

        ChatMessageResponse response = chatService.saveMessage(roomId, request, email);

        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);
    }
}