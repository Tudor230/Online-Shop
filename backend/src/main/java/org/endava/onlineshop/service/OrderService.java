package org.endava.onlineshop.service;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.CreateOrderRequestDto;
import org.endava.onlineshop.model.dto.OrderResponseDto;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.model.mapper.OrderMapper;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper = new OrderMapper();

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto requestDto) {
        if (requestDto.productIds() == null || requestDto.productIds().isEmpty()) {
            throw new BadRequestException("At least one product is required to create an order");
        }

        Function<Long, User> userResolver = userId -> userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));
        Function<Long, Product> productResolver = productId -> productRepository.findById(productId)
                .orElseThrow(() -> new BadRequestException("Product not found for id: " + productId));

        Order order = orderMapper.toOrderEntity(requestDto, userResolver, productResolver);
        return orderMapper.toOrderDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found for id: " + orderId));
        return orderMapper.toOrderDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        return orderRepository.findByUser(user).stream()
                .map(orderMapper::toOrderDto)
                .toList();
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found for id: " + orderId));
        order.setOrderStatus(status);
        return orderMapper.toOrderDto(orderRepository.save(order));
    }
}
