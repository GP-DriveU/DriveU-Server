package com.driveu.server.domain.user;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;
import com.driveu.server.domain.user.domain.User;

import java.time.LocalDateTime;

public class TestUserFactory {
    public static User getTestUser() {
        return new User(
                "testuser@example.com",
                "테스트유저",
                OauthProvider.GOOGLE,
                0L,
                5368709120L
        );
    }
}
