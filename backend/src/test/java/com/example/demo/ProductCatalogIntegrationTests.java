package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ProductCatalogInitializer catalogInitializer;

    /** 初期カテゴリーと商品が一度だけ登録されることを検証する。 */
    @Test
    void initialCatalogContainsSixProductsAndIsIdempotent() throws Exception {
        assertThat(categories.count()).isEqualTo(3);
        assertThat(products.count()).isEqualTo(6);

        catalogInitializer.run();

        assertThat(categories.count()).isEqualTo(3);
        assertThat(products.count()).isEqualTo(6);
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
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].productCode").value("vitamin-c"))
                .andExpect(jsonPath("$[0].category").value("サプリメント"));
    }

    /** 商品管理APIが管理者だけに登録・更新を許可することを検証する。 */
    @Test
    void productManagementRequiresAdministratorAndCanCreateAndUpdateProducts() throws Exception {
        String productJson = """
                {"name":"テスト青汁","category":"食品・飲料","description":"国産野菜の青汁です。",
                 "price":1200,"stockQuantity":25,"imageUrl":"","active":true}
                """;
        api.perform(get("/api/admin/products").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        api.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("テスト青汁"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.active").value(true));

        Product created = products.findAllByOrderById().getLast();
        String updateJson = """
                {"name":"テスト青汁 改良版","category":"食品・飲料","description":"説明を更新しました。",
                 "price":1350,"stockQuantity":10,"imageUrl":"https://example.com/aojiru.png","active":false}
                """;
        api.perform(put("/api/admin/products/{id}", created.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1350))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.active").value(false));
        api.perform(get("/api/products").with(user("customer")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("テスト青汁 改良版"))));
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

        assertThat(orderItems.findAllByOrderId(order.getId()).getFirst())
                .extracting(OrderItem::getProductName, OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactly(orderedProductName, orderedUnitPrice, 2);
        assertThat(orders.findAllByCustomerCustomerIdOrderByOrderedAtDesc(customer.getCustomerId()))
                .containsExactly(order);
    }
}
