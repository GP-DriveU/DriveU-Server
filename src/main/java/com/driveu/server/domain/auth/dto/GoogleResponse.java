package com.driveu.server.domain.auth.dto;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;

import java.util.Map;

public class GoogleResponse implements OauthResponse {

    // Json 응답 형식: resultcode=00, message=success, id=123123123, name=박연주
    private final Map<String, Object> attribute;

    public GoogleResponse(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public OauthProvider getProvider() {
        return OauthProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return attribute.get("sub").toString();
    }

    @Override
    public String getEmail() {
        return attribute.get("email").toString();
    }

    @Override
    public String getName() {
        return attribute.get("name").toString();
    }
}
