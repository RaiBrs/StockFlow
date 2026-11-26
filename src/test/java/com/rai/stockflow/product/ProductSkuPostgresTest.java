package com.rai.stockflow.product;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Confere o índice real: a consulta prévia do serviço não protege gravações simultâneas. */
class ProductSkuPostgresTest {

    @Test
    void databaseRejectsSkuThatDiffersOnlyByCase() throws Exception {
        String database = System.getenv("DB_NAME");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        assumeTrue(database != null && !database.isBlank()
                && user != null && !user.isBlank() && password != null,
                "Configure DB_NAME, DB_USER e DB_PASSWORD para executar no PostgreSQL.");

        String url = System.getenv().getOrDefault("STOCKFLOW_TEST_DB_URL",
                "jdbc:postgresql://localhost:5432/" + database);
        String sku = "TEST-" + UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                insert(connection, sku);
                SQLException error = assertThrows(SQLException.class,
                        () -> insert(connection, sku.toLowerCase(java.util.Locale.ROOT)));
                assertEquals("23505", error.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }

    private void insert(Connection connection, String sku) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO products (sku, name, category) VALUES (?, ?, ?)")) {
            statement.setString(1, sku);
            statement.setString(2, "Produto de teste");
            statement.setString(3, "Teste");
            statement.executeUpdate();
        }
    }
}
