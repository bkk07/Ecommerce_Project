package com.ecommerce.notificationservice;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.service.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EnableDiscoveryClient
@SpringBootApplication
public class NotificationServiceApplication {

	public static void main(String[] args) {
//		SpringApplication.run(NotificationServiceApplication.class, args);
        ConfigurableApplicationContext context =
                SpringApplication.run(NotificationServiceApplication.class, args);

        TemplateRepositoryPort templateRepository =
                context.getBean(TemplateRepositoryPort.class);

        NotificationService notificationService =context.getBean(NotificationService.class);
        Optional<NotificationTemplate> template =
                templateRepository.findByEventTypeAndChannel(
                        "EMAIL_VERIFICATION",
                        ChannelType.EMAIL
                );
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setEventType("EMAIL_VERIFICATION");
        notificationRequest.setChannelType(ChannelType.EMAIL);
        notificationRequest.setRecipient("pomema2818@m3player.com");
        Map<String ,String> map = new HashMap<>();
        map.put("name","Kiran");
        map.put("link","www.jaideep.com");
        notificationRequest.setParams(map);
        notificationService.processNotification(notificationRequest);
	}

}
