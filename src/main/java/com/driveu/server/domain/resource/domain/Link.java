package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.resource.domain.type.IconType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "link")
public class Link extends Resource {

    @Column(name = "url")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "icon_type")
    private IconType iconType;

    @Builder
    private Link(String title, String url, IconType iconType) {
        super(title);
        this.url = url;
        this.iconType = iconType;
    }

    public static Link of(String title, String url, IconType iconType) {
        return Link.builder()
                .title(title)
                .url(url)
                .iconType(iconType)
                .build();
    }
}
