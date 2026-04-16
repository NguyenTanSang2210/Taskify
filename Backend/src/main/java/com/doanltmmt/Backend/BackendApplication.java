package com.doanltmmt.Backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(BackendApplication.class, args);
    }

    // Tạo dữ liệu mẫu khi app khởi động
    @Bean
    CommandLineRunner initData(ObjectProvider<DataSeeder> seederProvider) {
        return args -> {
            DataSeeder seeder = seederProvider.getIfAvailable();
            if (seeder != null) {
                seeder.seed();
            }
        };
    }
}
