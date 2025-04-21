package com.centralconsig.crawler_bancos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CentralconsigCrawlerBancosApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralconsigCrawlerBancosApplication.class, args);
	}

}
