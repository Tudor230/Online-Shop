package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.CmsPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CmsPageRepository extends JpaRepository<CmsPage, UUID> {

    Optional<CmsPage> findBySlug(String slug);

    Optional<CmsPage> findBySlugAndIsPublishedTrue(String slug);
}
