package com.driveu.server.domain.resource.domain.type;

public enum IconType {
    YOUTUBE, GITHUB, DEFAULT;

    public static IconType fromUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (url.startsWith("https://github.com/")) {
            return IconType.GITHUB;
        } else if (url.startsWith("https://www.youtube.com/")) {
            return IconType.YOUTUBE;
        } else {
            return IconType.DEFAULT;
        }
    }
}
