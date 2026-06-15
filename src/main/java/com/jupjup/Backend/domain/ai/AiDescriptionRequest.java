package com.jupjup.Backend.domain.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiDescriptionRequest {
    private String title;
    private String category;
    private String memo;
}
