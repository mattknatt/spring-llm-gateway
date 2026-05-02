package org.example.springllmgateway;

import org.example.springllmgateway.config.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class SpringLlmGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringLlmGatewayApplication.class, args);
    }

}
