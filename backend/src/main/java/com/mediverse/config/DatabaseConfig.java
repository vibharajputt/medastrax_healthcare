package com.mediverse.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:}")
    private String databaseUsername;

    @Value("${DATABASE_PASSWORD:}")
    private String databasePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String rawUrl = (databaseUrl != null && !databaseUrl.trim().isEmpty()) 
            ? databaseUrl.trim() 
            : "jdbc:postgresql://localhost:5432/mediversedb";

        String jdbcUrl = rawUrl;
        String user = (databaseUsername != null && !databaseUsername.trim().isEmpty()) ? databaseUsername.trim() : null;
        String pass = (databasePassword != null && !databasePassword.trim().isEmpty()) ? databasePassword.trim() : null;

        // If the URL starts with postgres:// or postgresql:// (e.g. from Neon or Render)
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (user == null) {
                        user = parts[0];
                    }
                    if (pass == null) {
                        pass = parts[1];
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isEmpty()) {
                    jdbcUrl += "?" + query;
                } else if (!jdbcUrl.contains("sslmode")) {
                    jdbcUrl += "?sslmode=require";
                }
            } catch (Exception e) {
                jdbcUrl = "jdbc:" + rawUrl;
            }
        } else if (rawUrl.startsWith("jdbc:postgresql://")) {
            // Check if credentials are embedded in jdbc:postgresql://user:pass@host:port/db
            int atIdx = rawUrl.indexOf('@');
            int slashSlashIdx = rawUrl.indexOf("//");
            if (atIdx != -1 && slashSlashIdx != -1 && atIdx > slashSlashIdx + 2) {
                String userInfo = rawUrl.substring(slashSlashIdx + 2, atIdx);
                if (userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (user == null) {
                        user = parts[0];
                    }
                    if (pass == null) {
                        pass = parts[1];
                    }
                }
                jdbcUrl = rawUrl.substring(0, slashSlashIdx + 2) + rawUrl.substring(atIdx + 1);
            }
        }

        // Local default credentials when connecting to localhost
        if (user == null && jdbcUrl.contains("localhost")) {
            user = "postgres";
            pass = "1234567890";
        }

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");
        if (user != null) {
            config.setUsername(user);
        }
        if (pass != null) {
            config.setPassword(pass);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);

        return new HikariDataSource(config);
    }
}
