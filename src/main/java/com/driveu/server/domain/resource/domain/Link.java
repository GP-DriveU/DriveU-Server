package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.resource.domain.type.IconType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "link")
public class Link extends Resource {

    @Column(name = "url")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "icon_type")
    private IconType iconType;

    public static Link of(String url, IconType iconType) {
        return Link.builder()
                .url(url)
                .iconType(iconType)
                .build();
    }
}
