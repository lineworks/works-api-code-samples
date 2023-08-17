package com.example.echobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@ConfigurationProperties(prefix="works.api")
record WorksApiParameters(
        String clientId,
        String clientSecret,
        String serviceAccount,
        String privateKey,
        String botId,
        String botSecret
        ){}


@SpringBootApplication
@EnableConfigurationProperties(WorksApiParameters.class)
public class EchobotApplication {

	public static void main(String[] args) {
		SpringApplication.run(EchobotApplication.class, args);
	}
}
