package com.jupjup.Backend.domain.user;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    private String email;
    private String password;
    private String nickname;
    private String location;
}