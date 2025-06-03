package com.driveu.server.domain.resource.domain.type;

public enum IconType {
    YOUTUBE, GITHUB;

    public static IconType fromUrl(String url) {
        if (url.startsWith("https://github.com/")) {
            return IconType.GITHUB;
        }
        return IconType.YOUTUBE;
    }
}
