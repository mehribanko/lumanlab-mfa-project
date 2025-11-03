package com.lumonlab.childcaremfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ChildcareMfaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChildcareMfaApplication.class, args);
    }

}
