package org.endava.onlineshop.model.mapper;

import org.endava.onlineshop.model.dto.CreateOrderRequestDto;
import org.endava.onlineshop.model.dto.OrderResponseDto;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;

import java.util.List;
import java.util.function.Function;

// TODO: use MapStruct
public class OrderMapper {
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    private OrderMapper(UserMapper userMapper, ProductMapper productMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public Order toOrderEntity(CreateOrderRequestDto dto, Function<Long, User> userMapper, Function<Long, Product> productMapper) {
        Order order = new Order();

        order.setUser(userMapper.apply(dto.userId()));
        order.getProducts().addAll(dto.productIds().stream().map(productMapper).toList());
        order.setOrderStatus(OrderStatus.PROCESSING);

        return order;
    }

    public OrderResponseDto toOrderDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                userMapper.toUserResponseDto(order.getUser()),
                order.getProducts()
                        .stream()
                        .map(productMapper::toProductDto)
                        .toList(),
                order.getOrderStatus()
        );
    }
}
