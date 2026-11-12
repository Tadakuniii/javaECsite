package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShoppingCartService {
    private final ShoppingCartRepository carts;
    private final CartItemRepository cartItems;
    private final CustomerRepository customers;
    private final ProductRepository products;

    public ShoppingCartService(ShoppingCartRepository carts, CartItemRepository cartItems,
                               CustomerRepository customers, ProductRepository products) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.customers = customers;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public ShoppingCartResponse getCart(String customerId) {
        return carts.findByCustomerCustomerId(customerId)
                .map(cart -> responseFor(cart.getId()))
                .orElseGet(() -> ShoppingCartResponse.from(java.util.List.of()));
    }

    @Transactional
    public ShoppingCartResponse addItem(String customerId, AddCartItemRequest request) {
        ShoppingCart cart = getOrCreateCart(customerId);
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        if (!product.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "現在販売していない商品です");
        }

        CartItem item = cartItems.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> new CartItem(cart, product, 0));
        long newQuantity = (long) item.getQuantity() + request.quantity();
        validateQuantity(product, newQuantity);
        item.setQuantity(Math.toIntExact(newQuantity));
        cartItems.save(item);
        return responseFor(cart.getId());
    }

    @Transactional
    public ShoppingCartResponse updateItem(String customerId, long itemId, CartItemQuantityRequest request) {
        ShoppingCart cart = findCartForUpdate(customerId);
        CartItem item = findOwnedItem(cart, itemId);
        validateQuantity(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        cartItems.save(item);
        return responseFor(cart.getId());
    }

    @Transactional
    public ShoppingCartResponse removeItem(String customerId, long itemId) {
        ShoppingCart cart = findCartForUpdate(customerId);
        cartItems.delete(findOwnedItem(cart, itemId));
        cartItems.flush();
        return responseFor(cart.getId());
    }

    private ShoppingCart getOrCreateCart(String customerId) {
        Customer customer = lockCustomer(customerId);
        return carts.findByCustomerCustomerId(customerId).orElseGet(() -> {
            return carts.save(new ShoppingCart(customer));
        });
    }

    private ShoppingCart findCartForUpdate(String customerId) {
        lockCustomer(customerId);
        return carts.findByCustomerCustomerId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "買い物カゴが見つかりません"));
    }

    private Customer lockCustomer(String customerId) {
        return customers.findByIdForCartUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください"));
    }

    private CartItem findOwnedItem(ShoppingCart cart, long itemId) {
        return cartItems.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "カゴの商品が見つかりません"));
    }

    private ShoppingCartResponse responseFor(long cartId) {
        return ShoppingCartResponse.from(cartItems.findAllByCartIdOrderById(cartId));
    }

    private void validateQuantity(Product product, long quantity) {
        if (!product.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "現在販売していない商品です");
        }
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数量は1以上にしてください");
        }
        if (quantity > product.getStockQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "在庫数を超えて追加できません");
        }
    }
}
