package org.endava.onlineshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.endava.onlineshop.model.entities.Address;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.AddressRepository;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// TODO: Remove before deploying to production.
@Component
public class OrderMockSeeder implements ApplicationRunner {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;

    public OrderMockSeeder(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Value("classpath:seed/mock-orders.json") Resource seedResource
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedOrder> templates = loadSeedOrders();
        userRepository.findAll().forEach(user -> seedForUserIfMissing(user, templates));
    }

    @Transactional
    public void seedForUserIfMissing(User user) {
        seedForUserIfMissing(user, loadSeedOrders());
    }

    private void seedForUserIfMissing(User user, List<SeedOrder> templates) {
        if (orderRepository.existsByUserId(user.getId())) {
            return;
        }

        List<Address> userAddresses = addressRepository.findByUserId(user.getId());
        Address defaultAddress = userAddresses.stream()
                .min(Comparator.comparing(Address::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseGet(() -> createDefaultAddress(user));

        if (user.getDefaultShippingAddressId() == null) {
            user.setDefaultShippingAddressId(defaultAddress.getId());
        }
        if (user.getDefaultBillingAddressId() == null) {
            user.setDefaultBillingAddressId(defaultAddress.getId());
        }
        userRepository.save(user);

        for (int index = 0; index < templates.size(); index++) {
            SeedOrder template = templates.get(index);
            Order order = new Order();
            order.setOrderNumber(template.orderNumber() + "-" + shortUserSuffix(user.getId()) + "-" + (index + 1));
            order.setUserId(user.getId());
            order.setGuestEmail(user.getEmail());
            order.setShippingAddressId(user.getDefaultShippingAddressId());
            order.setBillingAddressId(user.getDefaultBillingAddressId());
            order.setCurrentStatus(template.status());

            BigDecimal subtotal = BigDecimal.ZERO;
            for (SeedOrderItem templateItem : template.items()) {
                Product product = productRepository.findBySlug(templateItem.productSlug())
                        .orElseThrow(() -> new IllegalStateException("Missing product in order seed: " + templateItem.productSlug()));

                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(templateItem.quantity());
                item.setUnitPriceAtPurchase(product.getBasePrice());
                order.addItem(item);

                subtotal = subtotal.add(product.getBasePrice().multiply(BigDecimal.valueOf(templateItem.quantity())));
            }

            BigDecimal discount = template.discountAmount();
            BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

            order.setSubtotal(subtotal);
            order.setDiscountAmount(discount);
            order.setTotalAmount(total);

            OrderStatusHistory historyItem = new OrderStatusHistory();
            historyItem.setStatus(template.status());
            historyItem.setNotes(template.statusNote());
            order.addStatusHistory(historyItem);

            orderRepository.save(order);
        }
    }

    private Address createDefaultAddress(User user) {
        Address address = new Address();
        address.setUserId(user.getId());
        address.setRecipientName((user.getFirstName() + " " + user.getLastName()).trim());
        address.setAddressLine1("123 Demo Street");
        address.setAddressLine2(null);
        address.setCity("Timisoara");
        address.setState("Timis");
        address.setPostalCode("300001");
        address.setCountry("Romania");
        address.setPhoneNumber("+40 700 000 000");
        return addressRepository.save(address);
    }

    private List<SeedOrder> loadSeedOrders() {
        try {
            return objectMapper.readValue(seedResource.getInputStream(), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load order seed data", ex);
        }
    }

    private String shortUserSuffix(UUID userId) {
        String value = userId.toString().replace("-", "");
        return value.substring(Math.max(0, value.length() - 6));
    }

    private record SeedOrder(
            String orderNumber,
            OrderStatus status,
            String statusNote,
            BigDecimal discountAmount,
            List<SeedOrderItem> items
    ) {
    }

    private record SeedOrderItem(
            String productSlug,
            int quantity
    ) {
    }
}
