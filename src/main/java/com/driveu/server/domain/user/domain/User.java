package com.driveu.server.domain.user.domain;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "user")
@Builder
public class User extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "o_auth_provider", nullable = false)
    private OauthProvider oauthProvider;

    @Column(name = "used_storage", nullable = false)
    @Builder.Default
    private Long usedStorage = 0L; // 현재 사용 중인 바이트(Byte) 단위 저장 용량. 기본 0

    @Column(name = "max_storage", nullable = false, updatable = false)
    @Builder.Default
    private Long maxStorage = 5368709120L;    // 이 유저에게 허용된 최대 저장 용량 (Byte 단위) -> 5GB

    public static User of(String name, String email, OauthProvider oauthProvider) {
        return User.builder()
                .name(name)
                .email(email)
                .oauthProvider(oauthProvider)
                .build();
    }

    public void setUsedStorage(long updatedUsedStorage) {
        this.usedStorage = updatedUsedStorage;
    }

    public Long getRemainingStorage() {
        return this.maxStorage - this.usedStorage;
    }
}
