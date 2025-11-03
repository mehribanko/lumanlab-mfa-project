package com.lumonlab.childcaremfa.feat.auth.dto;

import com.lumonlab.childcaremfa.feat.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Long userId;
    private String email;
    private Set<Role> roles;
    private Boolean mfaEnabled;
    private Boolean mfaRequired;
    private String message;


}