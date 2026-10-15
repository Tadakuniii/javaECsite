package com.example.demo;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class CustomerCreatedAtMigration implements CommandLineRunner {
    private final JdbcTemplate database;

    public CustomerCreatedAtMigration(JdbcTemplate database) {
        this.database = database;
    }

    /** 既存顧客の作成日時を直接補完してから、DBのNOT NULL制約を適用する。 */
    @Override
    public void run(String... args) {
        database.update("UPDATE customers SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        database.execute((Connection connection) -> {
            enforceNotNullConstraint(connection);
            return null;
        });
    }

    /** テスト用H2と運用用MySQLの構文差を吸収して同じ制約を設定する。 */
    private void enforceNotNullConstraint(Connection connection) throws SQLException {
        String databaseName = connection.getMetaData().getDatabaseProductName();
        String sql = databaseName.equalsIgnoreCase("MySQL")
                ? "ALTER TABLE customers MODIFY COLUMN created_at DATETIME(6) NOT NULL"
                : "ALTER TABLE customers ALTER COLUMN created_at SET NOT NULL";
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
