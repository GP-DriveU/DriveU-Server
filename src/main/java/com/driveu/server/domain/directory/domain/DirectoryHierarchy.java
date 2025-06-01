package com.driveu.server.domain.directory.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "directory_hierarchy")
public class DirectoryHierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    @Column(name = "depth", nullable = false)
    private int depth; // 자기 자신이면 0, 자식이면 1, 손자면 2

    @Builder
    private DirectoryHierarchy(Long ancestorId, Long descendantId, int depth) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
        this.depth = depth;
    }

    public static DirectoryHierarchy of(Long ancestorId, Long descendantId, int depth) {
        return DirectoryHierarchy.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .depth(depth)
                .build();
    }
}

