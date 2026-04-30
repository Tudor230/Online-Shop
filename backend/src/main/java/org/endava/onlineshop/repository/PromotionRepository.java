package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCode(String code);

    Page<Promotion> findByIsActiveTrue(Pageable pageable);

    Page<Promotion> findByIsActiveFalse(Pageable pageable);

    boolean existsByCode(String code);
}
