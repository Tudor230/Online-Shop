package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findById(Long id);

    List<Order> findByUser(User user);
}
