package com.unloadbrain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.unloadbrain.annotation.GenerateEntity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@SpringBootApplication
@EnableJpaRepositories
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner runner(CustomerStoRepository repository) {
        return (args) -> {
            repository.save(new CustomerSto("1", "1", "John", "Doe"));
            repository.save(new CustomerSto("1", "2", "Jane", "Doe"));

            log.info("CustomerSto found with findAll(): {}", repository.findAll());
        };
    }

}

@Data
@Entity
@GenerateEntity(packageName = "com.unloadbrain", entityName = "CustomerSto", tableName = "customer_sto")
class Customer {

    @Id
    private String id;
    private String firstName;
    private String lastName;

    public Customer(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}