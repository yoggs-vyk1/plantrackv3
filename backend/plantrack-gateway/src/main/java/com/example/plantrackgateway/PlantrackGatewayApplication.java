package com.example.plantrackgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PlantrackGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlantrackGatewayApplication.class, args);
	}

}
