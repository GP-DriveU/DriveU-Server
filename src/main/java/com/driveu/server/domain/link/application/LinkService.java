package com.driveu.server.domain.link.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.link.dao.LinkRepository;
import com.driveu.server.domain.resource.domain.Link;
import com.driveu.server.domain.resource.domain.type.IconType;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;
    private final DirectoryService directoryService;

    @Transactional
    public Long saveLink(Long directoryId, LinkSaveRequest request) {
        Directory directory = directoryService.getDirectoryById(directoryId);

        Directory tagDirectory = null;
        if (request.getTagId() != null) {
            tagDirectory = directoryService.getDirectoryById(request.getTagId());
        }

        IconType iconType = IconType.fromUrl(request.getUrl().trim());
        Link link = Link.of(request.getTitle(), request.getUrl().trim(), iconType);

        // 디렉토리 연결
        link.addDirectory(directory);
        if (tagDirectory != null) {
            link.addDirectory(tagDirectory);
        }

        Link saved = linkRepository.save(link);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public String getLinkUrl(Long linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link not found."));

        return link.getUrl();
    }
}
