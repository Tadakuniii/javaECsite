package com.example.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureMockMvc
class ProductCatalogIntegrationTests {
    @Autowired MockMvc api;
    @Autowired ProductCategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired ShoppingCartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired PurchaseOrderRepository purchaseOrders;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ProductCatalogInitializer catalogInitializer;
    @Autowired ShoppingCartService shoppingCartService;
    @Autowired JdbcTemplate database;
    @PersistenceContext EntityManager entityManager;

    /** 初期カテゴリーと商品が一度だけ登録されることを検証する。 */
    @Test
    @Transactional
    void initialCatalogContainsSixProductsAndIsIdempotent() throws Exception {
        assertThat(categories.count()).isEqualTo(3);
        assertThat(products.count()).isEqualTo(6);

        Product existingProduct = products.findAllByActiveTrueOrderById().getFirst();
        existingProduct.setName("カスタム商品名");
        existingProduct.setPrice(9999);
        products.saveAndFlush(existingProduct);

        catalogInitializer.run();

        assertThat(categories.count()).isEqualTo(3);
        assertThat(products.count()).isEqualTo(6);
        Product reloadedProduct = products.findById(existingProduct.getId()).orElseThrow();
        assertThat(reloadedProduct.getName()).isEqualTo("カスタム商品名");
        assertThat(reloadedProduct.getPrice()).isEqualTo(9999);
        assertThat(products.findAllByActiveTrueOrderById())
                .allSatisfy(product -> {
                    assertThat(product.getPrice()).isGreaterThanOrEqualTo(0);
                    assertThat(product.getStockQuantity()).isGreaterThanOrEqualTo(0);
                    assertThat(product.getCategory()).isNotNull();
                });
    }

    /** 商品APIが未認証を拒否し、認証後はDBの商品を返すことを検証する。 */
    @Test
    void productApiRequiresLoginAndReturnsDatabaseCatalog() throws Exception {
        api.perform(get("/api/products")).andExpect(status().isUnauthorized());
        api.perform(get("/api/products").with(user("catalog-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.products.length()").value(6))
                .andExpect(jsonPath("$.products[0].productCode").value("vitamin-c"))
                .andExpect(jsonPath("$.products[0].category").value("サプリメント"));
    }

    /** 顧客作成日時の移行後にNULL値がなく、DB制約も有効なことを検証する。 */
    @Test
    void customerCreatedAtMigrationBackfillsValuesBeforeEnforcingConstraint() {
        Integer missingTimestamps = database.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE created_at IS NULL", Integer.class);
        String nullable = database.queryForObject("""
                SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'CUSTOMERS' AND COLUMN_NAME = 'CREATED_AT'
                """, String.class);

        assertThat(missingTimestamps).isZero();
        assertThat(nullable).isEqualTo("NO");
    }

    /** 商品管理APIが管理者だけに登録・更新を許可することを検証する。 */
    @Test
    void productManagementRequiresAdministratorAndCanCreateAndUpdateProducts() throws Exception {
        String productJson = """
                {"name":"テスト青汁","category":"食品・飲料","description":"国産野菜の青汁です。",
                 "sizeDescription":"30包","price":1200,"stockQuantity":25,"imageUrl":"","active":true,
                 "autoOrderEnabled":false,"reorderThreshold":0,"reorderQuantity":1,"defaultSupplierName":"",
                 "defaultOrderUnitCost":0,"defaultLeadTimeDays":7}
                """;
        api.perform(get("/api/admin/products").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        api.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("テスト青汁"))
                .andExpect(jsonPath("$.sizeDescription").value("30包"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.active").value(true));

        Product created = products.findAllByOrderById().getLast();
        String updateJson = """
                {"name":"テスト青汁 改良版","category":"食品・飲料","description":"説明を更新しました。",
                 "sizeDescription":"45包","price":1350,"stockQuantity":10,"imageUrl":"https://example.com/aojiru.png","active":false,
                 "autoOrderEnabled":false,"reorderThreshold":0,"reorderQuantity":1,"defaultSupplierName":"",
                 "defaultOrderUnitCost":0,"defaultLeadTimeDays":7}
                """;
        api.perform(put("/api/admin/products/{id}", created.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1350))
                .andExpect(jsonPath("$.sizeDescription").value("45包"))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.active").value(false));
        api.perform(get("/api/products").with(user("customer")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("テスト青汁 改良版"))));
    }

    /** ログインユーザーが商品を追加し、数量変更と削除を行えることを検証する。 */
    @Test
    void shoppingCartKeepsItemsPerCustomerAndCalculatesTotals() throws Exception {
        customers.save(new Customer("cart-customer", passwordEncoder.encode("password123"), "カゴ利用者"));
        customers.save(new Customer("other-customer", passwordEncoder.encode("password123"), "別の利用者"));
        Product product = products.findAllByActiveTrueOrderById().getFirst();
        String addJson = "{\"productId\":" + product.getId() + ",\"quantity\":1}";

        api.perform(post("/api/cart/items")
                        .with(user("cart-customer")).with(csrf())
                        .contentType("application/json").content(addJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(1))
                .andExpect(jsonPath("$.totalPrice").value(product.getPrice()))
                .andExpect(jsonPath("$.items[0].name").value(product.getName()))
                .andReturn();
        ShoppingCart createdCart = carts.findByCustomerCustomerId("cart-customer").orElseThrow();
        long itemId = cartItems.findAllByCartIdOrderById(createdCart.getId()).getFirst().getId();

        api.perform(post("/api/cart/items")
                        .with(user("cart-customer")).with(csrf())
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":2147483647}"))
                .andExpect(status().isBadRequest());

        api.perform(put("/api/cart/items/{itemId}", itemId)
                        .with(user("cart-customer")).with(csrf())
                        .contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(product.getPrice() * 3));
        api.perform(delete("/api/cart/items/{itemId}", itemId)
                        .with(user("other-customer")).with(csrf()))
                .andExpect(status().isNotFound());
        api.perform(delete("/api/cart/items/{itemId}", itemId)
                        .with(user("cart-customer")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    /** 購入時に注文と明細を保存し、在庫とカゴを同時に更新することを検証する。 */
    @Test
    void checkoutCreatesOrderHistoryReducesStockAndClearsCart() throws Exception {
        String customerId = "order-customer";
        customers.save(new Customer(customerId, passwordEncoder.encode("password123"), "注文利用者"));
        Product product = products.findAllByActiveTrueOrderById().getFirst();
        int stockBeforePurchase = product.getStockQuantity();
        int priceAtPurchase = product.getPrice();

        api.perform(post("/api/cart/items").with(user(customerId)).with(csrf())
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":2}"))
                .andExpect(status().isOk());

        api.perform(post("/api/orders").with(user(customerId)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.statusLabel").value("購入済み"))
                .andExpect(jsonPath("$.totalAmount").value(priceAtPurchase * 2))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        ShoppingCart cart = carts.findByCustomerCustomerId(customerId).orElseThrow();
        assertThat(cartItems.findAllByCartIdOrderById(cart.getId())).isEmpty();
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(stockBeforePurchase - 2);
        assertThat(orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customerId)).hasSize(1);

        api.perform(get("/api/orders").with(user(customerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].unitPrice").value(priceAtPurchase));
        api.perform(get("/api/orders").with(user("other-customer")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        api.perform(post("/api/orders").with(user(customerId)).with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customerId)).hasSize(1);
    }

    /** 手動発注と、在庫減少による自動発注、入荷時の在庫反映を検証する。 */
    @Test
    void procurementCreatesManualAndAutomaticOrdersAndReceivesStock() throws Exception {
        String customerId = "procurement-customer";
        customers.save(new Customer(customerId, passwordEncoder.encode("password123"), "発注テスト利用者"));
        Product product = products.findAllByActiveTrueOrderById().getLast();
        Product manuallyOrderedProduct = products.findAllByActiveTrueOrderById().getFirst();
        int stockBeforePurchase = product.getStockQuantity();
        product.setAutoOrderEnabled(true);
        product.setReorderThreshold(stockBeforePurchase - 1);
        product.setReorderQuantity(8);
        product.setDefaultSupplierName("自動発注テスト商事");
        product.setDefaultOrderUnitCost(450);
        product.setDefaultLeadTimeDays(3);
        products.saveAndFlush(product);

        String manualOrderJson = """
                {"productId":%d,"supplierName":"手動発注テスト商事","expectedDeliveryDate":"%s",
                 "unitCost":500,"quantity":4}
                """.formatted(manuallyOrderedProduct.getId(), java.time.LocalDate.now().plusDays(2));
        api.perform(post("/api/admin/procurements").with(user("customer").roles("CUSTOMER")).with(csrf())
                        .contentType("application/json").content(manualOrderJson))
                .andExpect(status().isForbidden());
        api.perform(post("/api/admin/procurements").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(manualOrderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierName").value("手動発注テスト商事"))
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.automatic").value(false));

        String pastDeliveryDateJson = """
                {"productId":%d,"supplierName":"期限切れテスト商事","expectedDeliveryDate":"%s",
                 "unitCost":500,"quantity":4}
                """.formatted(manuallyOrderedProduct.getId(), java.time.LocalDate.now().minusDays(1));
        api.perform(post("/api/admin/procurements").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(pastDeliveryDateJson))
                .andExpect(status().isBadRequest());

        String outOfRangeDeliveryDateJson = """
                {"productId":%d,"supplierName":"日付上限テスト商事","expectedDeliveryDate":"+10000-01-01",
                 "unitCost":500,"quantity":4}
                """.formatted(manuallyOrderedProduct.getId());
        api.perform(post("/api/admin/procurements").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(outOfRangeDeliveryDateJson))
                .andExpect(status().isBadRequest());

        String invalidLeadTimeJson = """
                {"name":"納期上限テスト商品","category":"食品・飲料","description":"納期上限の検証用です。",
                 "sizeDescription":"1個","price":100,"stockQuantity":0,"imageUrl":"","active":true,
                 "autoOrderEnabled":true,"reorderThreshold":1,"reorderQuantity":1,
                 "defaultSupplierName":"納期テスト商事","defaultOrderUnitCost":50,
                 "defaultLeadTimeDays":2147483647}
                """;
        api.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(invalidLeadTimeJson))
                .andExpect(status().isBadRequest());

        api.perform(post("/api/cart/items").with(user(customerId)).with(csrf())
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        api.perform(post("/api/orders").with(user(customerId)).with(csrf()))
                .andExpect(status().isOk());

        List<PurchaseOrder> automaticOrders = purchaseOrders.findAllByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getProduct().getId().equals(product.getId()) && order.isAutomatic())
                .toList();
        assertThat(automaticOrders).singleElement().satisfies(order -> {
            assertThat(order.getSupplierName()).isEqualTo("自動発注テスト商事");
            assertThat(order.getQuantity()).isEqualTo(8);
            assertThat(order.getUnitCost()).isEqualTo(450);
        });

        api.perform(post("/api/cart/items").with(user(customerId)).with(csrf())
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        api.perform(post("/api/orders").with(user(customerId)).with(csrf()))
                .andExpect(status().isOk());
        assertThat(purchaseOrders.findAllByOrderByOrderedAtDesc().stream()
                .filter(order -> order.getProduct().getId().equals(product.getId()) && order.isAutomatic()))
                .hasSize(1);

        PurchaseOrder automaticOrder = automaticOrders.getFirst();
        api.perform(post("/api/admin/procurements/{id}/receive", automaticOrder.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(stockBeforePurchase - 2 + 8);
        api.perform(get("/api/admin/procurements").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("自動発注テスト商事")));
    }

    /** 初回カゴ作成と同じ商品の追加が同時でも、1つのカゴへ数量が加算されることを検証する。 */
    @Test
    void concurrentAddsCreateOneCartWithoutLosingQuantity() throws Exception {
        String customerId = "concurrent-cart-customer";
        customers.save(new Customer(customerId, passwordEncoder.encode("password123"), "同時追加利用者"));
        Product product = products.findAllByActiveTrueOrderById().getFirst();
        AddCartItemRequest request = new AddCartItemRequest(product.getId(), 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ShoppingCartResponse> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("開始待機がタイムアウトしました");
                return shoppingCartService.addItem(customerId, request);
            });
            Future<ShoppingCartResponse> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("開始待機がタイムアウトしました");
                return shoppingCartService.addItem(customerId, request);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        ShoppingCart cart = carts.findByCustomerCustomerId(customerId).orElseThrow();
        assertThat(carts.findAll()).filteredOn(savedCart -> savedCart.getCustomer().getCustomerId().equals(customerId))
                .hasSize(1);
        assertThat(cartItems.findAllByCartIdOrderById(cart.getId())).singleElement()
                .extracting(CartItem::getQuantity).isEqualTo(2);
    }

    /** カート関連と注文時の商品情報の保存を検証する。 */
    @Test
    @Transactional
    void cartAndOrderTablesPreserveRelationshipsAndOrderSnapshot() {
        Customer customer = customers.save(new Customer(
                "catalog-test", passwordEncoder.encode("password123"), "商品テスト利用者"));
        Product product = products.findAllByActiveTrueOrderById().getFirst();

        ShoppingCart cart = carts.save(new ShoppingCart(customer));
        CartItem cartItem = cartItems.save(new CartItem(cart, product, 2));
        assertThat(cartItems.findAllByCartId(cart.getId())).containsExactly(cartItem);
        assertThat(carts.findByCustomerCustomerId(customer.getCustomerId())).contains(cart);

        CustomerOrder order = orders.save(new CustomerOrder(customer, product.getPrice() * 2));
        OrderItem orderItem = orderItems.save(new OrderItem(order, product, 2));
        String orderedProductName = orderItem.getProductName();
        String orderedSizeDescription = orderItem.getSizeDescription();
        int orderedUnitPrice = orderItem.getUnitPrice();

        product.setName("変更後の商品名");
        product.setSizeDescription("変更後の内容量");
        product.setPrice(product.getPrice() + 500);
        products.save(product);
        entityManager.flush();
        entityManager.detach(orderItem);

        assertThat(orderItems.findAllByOrderId(order.getId()).getFirst())
                .extracting(OrderItem::getProductName, OrderItem::getSizeDescription,
                        OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactly(orderedProductName, orderedSizeDescription, orderedUnitPrice, 2);
        assertThat(orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customer.getCustomerId()))
                .containsExactly(order);
    }
}
