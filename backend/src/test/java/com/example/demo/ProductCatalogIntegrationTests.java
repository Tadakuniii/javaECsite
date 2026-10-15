package com.example.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ProductCatalogInitializer catalogInitializer;
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
        int orderedUnitPrice = orderItem.getUnitPrice();

        product.setName("変更後の商品名");
        product.setPrice(product.getPrice() + 500);
        products.save(product);
        entityManager.flush();
        entityManager.detach(orderItem);

        assertThat(orderItems.findAllByOrderId(order.getId()).getFirst())
                .extracting(OrderItem::getProductName, OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactly(orderedProductName, orderedUnitPrice, 2);
        assertThat(orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customer.getCustomerId()))
                .containsExactly(order);
    }
}
