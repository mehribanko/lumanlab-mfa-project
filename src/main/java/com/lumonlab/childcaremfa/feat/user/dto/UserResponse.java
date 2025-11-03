package com.lumonlab.childcaremfa.feat.user.dto;

import com.lumonlab.childcaremfa.feat.user.entity.Role;
import com.lumonlab.childcaremfa.feat.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private UserStatus status;
    private Boolean mfaEnabled;
    private Set<Role> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}