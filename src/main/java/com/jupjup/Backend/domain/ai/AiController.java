package com.jupjup.Backend.domain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @PostMapping("/description")
    public ResponseEntity<Map<String, String>> generateDescription(@RequestBody AiDescriptionRequest request) {
        String prompt = String.format(
                "당신은 중고거래 플랫폼의 판매 상품 설명 작성 전문가입니다.\n" +
                "상품명: %s\n" +
                "카테고리: %s\n" +
                "판매자 메모: %s\n\n" +
                "위 정보를 바탕으로 중고거래 판매 설명을 한국어로 3~5문장 안에 작성해주세요.\n" +
                "- 상품 상태, 특징, 판매 이유를 자연스럽게 설명\n" +
                "- 직거래 환영 멘트 포함\n" +
                "- 설명만 출력 (다른 안내문 불필요)",
                request.getTitle(),
                request.getCategory() != null ? request.getCategory() : "미선택",
                request.getMemo() != null ? request.getMemo() : "없음"
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        List<Map> candidates = (List<Map>) response.getBody().get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        return ResponseEntity.ok(Map.of("description", text.trim()));
    }
}
