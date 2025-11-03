package com.lumonlab.childcaremfa.feat.social.dto;

import com.lumonlab.childcaremfa.feat.user.entity.SocialAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccResponse {

    private Long id;
    private SocialAuthProvider provider;
    private String email;
    private String displayName;
    private LocalDateTime linkedAt;
}
