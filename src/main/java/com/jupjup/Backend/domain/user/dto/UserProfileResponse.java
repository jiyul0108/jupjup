package com.jupjup.Backend.domain.user.dto;

import com.jupjup.Backend.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String location;
    private int jupjupScore;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getLocation(),
                user.getJupjupScore()
        );
    }
}
