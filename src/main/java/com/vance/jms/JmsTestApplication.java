package com.vance.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

import lombok.extern.slf4j.Slf4j;

/**
 * JMS 測試應用程式
 * 使用 IBM MQ JMS Spring Boot Starter 實現訊息傳送及接收功能
 */
@Slf4j
@EnableJms
@SpringBootApplication
public class JmsTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(JmsTestApplication.class, args);
		log.info("JMS 測試應用程式啟動完成!!");
		log.info("您可以使用 POST /api/messages/send 或 POST /api/messages/send-text 發送訊息");
	}

}
