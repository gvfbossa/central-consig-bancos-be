package com.centralconsig.crawler_bancos.application.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseInitializer {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cliente")) {

            rs.next();
            int count = rs.getInt(1);

            if (count == 0) {
                System.out.println("Tabela vazia, importando dump...");
                executarDump(conn);
            } else {
                System.out.println("Dados já presentes no banco.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executarDump(Connection conn) {
        try {
            InputStream is = getClass().getResourceAsStream("/sql/dump.sql");
            if (is == null) throw new RuntimeException("Dump SQL não encontrado");

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(statement);
                    }
                }
            }

            System.out.println("Dump executado com sucesso!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar dump", e);
        }
    }
}
