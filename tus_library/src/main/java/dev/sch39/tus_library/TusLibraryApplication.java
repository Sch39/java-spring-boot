package dev.sch39.tus_library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TusLibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TusLibraryApplication.class, args);
	}

}
