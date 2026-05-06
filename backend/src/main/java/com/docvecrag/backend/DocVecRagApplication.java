package com.docvecrag.backend;

import com.docvecrag.backend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DocVecRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocVecRagApplication.class, args);
    }
}
