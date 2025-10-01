package com.cathay.cdc.thumbnail.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@SpringBootApplication(scanBasePackages = "com.cathay.cdc.thumbnail.poc")
@EnableJpaRepositories("com.cathay.cdc.thumbnail.poc.repository")
@EntityScan("com.cathay.cdc.thumbnail.poc.entity")
@EnableScheduling
public class ThumbnailPocApplication {
	public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ThumbnailPocApplication.class);
        // Override server.port from environment variable if present
        String port = System.getenv("PORT");
        if (port != null) {
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }
        app.run(args);
	}
}
