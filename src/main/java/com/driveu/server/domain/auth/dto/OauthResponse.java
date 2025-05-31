package com.driveu.server.domain.auth.dto;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;

public interface OauthResponse {

    //제공자 (Ex. naver, google, ...)
    OauthProvider getProvider();
    //제공자에서 발급해주는 아이디(번호)
    String getProviderId();
    //이메일
    String getEmail();
    //사용자 이름
    String getName();
}
