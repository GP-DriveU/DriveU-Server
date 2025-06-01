package com.driveu.server.domain.directory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryOrderPair {
    private Long directoryId;
    private int order;
}
