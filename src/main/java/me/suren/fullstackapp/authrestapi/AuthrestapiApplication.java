package me.suren.fullstackapp.authrestapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "me.suren.fullstackapp")
@ConfigurationPropertiesScan(basePackages = "me.suren.fullstackapp")
public class AuthrestapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthrestapiApplication.class, args);
	}

}
