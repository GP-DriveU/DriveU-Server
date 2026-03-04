package com.driveu.server.domain.wal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Getter
@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalLogPayload {

    // 공통
    private Long targetId;

    // CREATE FILE / UPDATE FILE
    private String title;
    private String extension;
    private Long size;
    private String s3Path;

    // CREATE/GET LINK
    private String url;
    private Long tagId;

    // CREATE/RENAME FOLDER
    private String name;
    private String oldName;
    private String newName;

    // MOVE FOLDER
    private Long oldParentDirectoryId;
    private Long newParentDirectoryId;

    // CREATE FOLDER / 공통 디렉토리 참조
    private Long parentDirectoryId;
    private Long directoryId;
    private Long userSemesterId;

    // UPDATE NOTE
    private String content;

    // 즐겨찾기
    private Boolean isFavorite;

    // 정렬
    private Integer order;
}