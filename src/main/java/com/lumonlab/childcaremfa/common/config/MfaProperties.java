package com.lumonlab.childcaremfa.common.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mfa")
@Getter
@Setter
public class MfaProperties {
    private String issuer;
    private Integer digits;
    private Integer period;
}