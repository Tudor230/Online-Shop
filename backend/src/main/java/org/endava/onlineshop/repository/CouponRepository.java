package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    Page<Coupon> findByIsActiveTrue(Pageable pageable);

    Page<Coupon> findByIsActiveFalse(Pageable pageable);

    boolean existsByCode(String code);
}
