package com.jupjup.Backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    private String nickname;
    private String location;
    private String currentPassword;
    private String newPassword;
}
