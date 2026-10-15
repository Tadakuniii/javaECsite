package com.example.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class ProductCatalogInitializer implements CommandLineRunner {
    private final ProductCategoryRepository categories;
    private final ProductRepository products;

    public ProductCatalogInitializer(ProductCategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    /** カテゴリーと初期商品を、未登録の場合だけ追加する。 */
    @Override
    @Transactional
    public void run(String... args) {
        Map<String, ProductCategory> categoryByName = new LinkedHashMap<>();
        for (String name : new String[]{"サプリメント", "食品・飲料", "健康グッズ"}) {
            categoryByName.put(name, categories.findByName(name).orElseGet(() -> categories.save(new ProductCategory(name))));
        }

        addProduct("vitamin-c", categoryByName.get("サプリメント"), "ビタミンC サプリメント",
                "ビタミンCを配合した、持ち運びしやすいボトルタイプのサプリメント。",
                1480, 100, "120粒", "VITAMIN C", "#d28d38", "bottle");
        addProduct("mineral-zinc", categoryByName.get("サプリメント"), "亜鉛 サプリメント",
                "亜鉛を配合したタブレットタイプ。シンプルなパッケージでお届けします。",
                1280, 100, "60粒", "ZINC", "#7881b9", "bottle");
        addProduct("soy-protein", categoryByName.get("食品・飲料"), "ソイプロテイン",
                "大豆由来のプロテイン。水や牛乳と混ぜて楽しめるプレーンタイプ。",
                2980, 50, "500g", "SOY PROTEIN", "#a68b73", "pouch");
        addProduct("herbal-tea", categoryByName.get("食品・飲料"), "ハーブティー",
                "カモミールとルイボスをブレンドした、香りを楽しむティーバッグ。",
                980, 80, "20袋", "HERBAL TEA", "#689b87", "tea");
        addProduct("daily-water-bottle", categoryByName.get("健康グッズ"), "ウォーターボトル",
                "日々の持ち歩きに使いやすい、目盛り付きの軽量ウォーターボトル。",
                1980, 40, "600mL", "DAILY WATER", "#6393b1", "water-bottle");
        addProduct("yoga-mat", categoryByName.get("健康グッズ"), "ヨガマット",
                "自宅でのストレッチやヨガに。落ち着いたカラーのマットです。",
                3480, 30, "厚さ6mm", "YOGA MAT", "#9c86b9", "mat");
    }

    private void addProduct(String code, ProductCategory category, String name, String description,
                            int price, int stock, String size, String label, String accentColor,
                            String illustrationType) {
        if (!products.existsByProductCode(code)) {
            products.save(new Product(code, category, name, description, price, stock,
                    size, label, accentColor, illustrationType));
        }
    }
}
