package com.driveu.server.domain.user.domain;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "o_auth_provider", nullable = false)
    private OauthProvider oauthProvider;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String name, String email, OauthProvider oauthProvider) {
        this.name = name;
        this.email = email;
        this.oauthProvider = oauthProvider;
    }

    public static User of(String name, String email, OauthProvider oauthProvider) {
        return User.builder()
                .name(name)
                .email(email)
                .oauthProvider(oauthProvider)
                .build();
    }

    // Entity 가 처음 저장될 때 자동으로 createdAt 필드에 현재 시각을 넣어줌
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
