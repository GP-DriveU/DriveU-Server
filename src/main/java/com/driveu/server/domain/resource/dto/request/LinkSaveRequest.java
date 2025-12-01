package com.driveu.server.domain.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkSaveRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String url;

    private Long tagId; // nullable

    public LinkSaveRequest(String title, String url) {
        this.title = title;
        this.url = url;
        this.tagId = null;
    }
}
