package com.driveu.server.domain.user.dto.response;

import com.driveu.server.domain.resource.dto.response.TagResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class TagInfo {
    private List<TagResponse> subjectTags;
    private List<TagResponse> categoryTags;
}
