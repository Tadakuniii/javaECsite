package com.example.demo;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductManagementService {
    private final ProductRepository products;
    private final ProductCategoryRepository categories;

    public ProductManagementService(ProductRepository products, ProductCategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    /** 管理画面の入力から商品を新規登録する。 */
    @Transactional
    public ManagedProductResponse create(ProductManagementRequest request) {
        ProductCategory category = findCategory(request.category());
        String productCode = createUniqueProductCode(request.name());
        Product product = new Product(productCode, category, request.name().strip(), request.description().strip(),
                request.price(), request.stockQuantity(), "未設定", request.name().strip(),
                "#7881b9", "bottle");
        applyEditableValues(product, request);
        return ManagedProductResponse.from(products.save(product));
    }

    /** 指定された既存商品の編集可能項目を更新する。 */
    @Transactional
    public ManagedProductResponse update(long productId, ProductManagementRequest request) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        product.setCategory(findCategory(request.category()));
        applyEditableValues(product, request);
        return ManagedProductResponse.from(products.save(product));
    }

    private ProductCategory findCategory(String categoryName) {
        return categories.findByName(categoryName.strip())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリを確認してください"));
    }

    private void applyEditableValues(Product product, ProductManagementRequest request) {
        product.setName(request.name().strip());
        product.setDescription(request.description().strip());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().strip());
        product.setActive(request.active());
    }

    private String createUniqueProductCode(String productName) {
        String base = Normalizer.normalize(productName, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) base = "product";
        if (base.length() > 80) base = base.substring(0, 80).replaceAll("-$", "");
        String candidate = base;
        int suffix = 2;
        while (products.existsByProductCode(candidate)) candidate = base + "-" + suffix++;
        return candidate;
    }
}
