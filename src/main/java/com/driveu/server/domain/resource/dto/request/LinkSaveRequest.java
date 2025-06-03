package com.driveu.server.domain.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkSaveRequest {
    private String title;
    private String url;
    private Long tagId; // nullable

    public LinkSaveRequest(String title, String url) {
        this.title = title;
        this.url = url;
        this.tagId = null;
    }
}
