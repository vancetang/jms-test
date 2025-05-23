package com.vance.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vance.jms.service.MqConnectionService;

import lombok.extern.slf4j.Slf4j;

/**
 * JMS 測試應用程式
 * 使用 IBM MQ JMS Spring Boot Starter 實現訊息傳送及接收功能
 */
@Slf4j
@EnableJms
@EnableScheduling
@SpringBootApplication
public class JmsTestApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(JmsTestApplication.class, args);
		log.info("JMS 測試應用程式啟動完成!!");
		log.info("您可以使用 POST /api/messages/send 或 POST /api/messages/send-text 發送訊息");

		// 在應用程式啟動後，啟動 JMS 監聽器
		try {
			MqConnectionService mqConnectionService = context.getBean(MqConnectionService.class);
			if (mqConnectionService.isConnected()) {
				JmsListenerEndpointRegistry registry = context.getBean(JmsListenerEndpointRegistry.class);
				log.info("應用程式啟動完畢，啟動 JMS 監聽器");
				log.info("當前已註冊的監聽器容器 ID: {}", registry.getListenerContainerIds());

				// 如果有註冊的監聽器，則啟動它們
				if (!registry.getListenerContainerIds().isEmpty()) {
					registry.start();
					log.info("JMS 監聽器已手動啟動");
				} else {
					log.warn("沒有找到已註冊的 JMS 監聽器");
				}
			} else {
				log.info("MQ 連接未建立，無法啟動 JMS 監聽器。");
			}
		} catch (Exception e) {
			log.error("手動啟動 JMS 監聽器失敗: {}", e.getMessage(), e);
		}
	}

}
