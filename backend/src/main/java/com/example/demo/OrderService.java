package com.example.demo;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final ShoppingCartRepository carts;
    private final CartItemRepository cartItems;
    private final CustomerRepository customers;
    private final ProductRepository products;

    public OrderService(CustomerOrderRepository orders, OrderItemRepository orderItems,
                        ShoppingCartRepository carts, CartItemRepository cartItems,
                        CustomerRepository customers, ProductRepository products) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.carts = carts;
        this.cartItems = cartItems;
        this.customers = customers;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getOrderHistory(String customerId) {
        return orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customerId).stream()
                .map(order -> OrderHistoryResponse.from(order, orderItems.findAllByOrderId(order.getId())))
                .toList();
    }

    /** 注文、在庫更新、カゴの消去を一つの取引として確定する。 */
    @Transactional
    public OrderHistoryResponse checkout(String customerId) {
        Customer customer = customers.findByIdForCartUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください"));
        ShoppingCart cart = carts.findByCustomerCustomerId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "買い物カゴは空です"));
        List<CartItem> currentItems = cartItems.findAllByCartIdOrderById(cart.getId());
        if (currentItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "買い物カゴは空です");
        }

        List<CartItem> itemsInLockOrder = currentItems.stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        int totalAmount = 0;
        for (CartItem item : itemsInLockOrder) {
            Product product = products.findByIdForOrder(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品が見つかりません"));
            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, product.getName() + "は現在販売していません");
            }
            if (item.getQuantity() > product.getStockQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, product.getName() + "の在庫が不足しています");
            }
            item.setProduct(product);
            totalAmount = Math.addExact(totalAmount, Math.multiplyExact(product.getPrice(), item.getQuantity()));
        }

        CustomerOrder order = new CustomerOrder(customer, totalAmount);
        order.setStatus(OrderStatus.PAID);
        orders.save(order);
        List<OrderItem> purchasedItems = currentItems.stream().map(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            return new OrderItem(order, product, item.getQuantity());
        }).toList();
        products.saveAll(currentItems.stream().map(CartItem::getProduct).toList());
        orderItems.saveAll(purchasedItems);
        cartItems.deleteAll(currentItems);
        cartItems.flush();
        return OrderHistoryResponse.from(order, purchasedItems);
    }
}
