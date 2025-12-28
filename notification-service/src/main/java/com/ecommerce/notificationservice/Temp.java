//package com.ecommerce.notificationservice;
//import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
//import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
//import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
//import com.ecommerce.notificationservice.dto.NotificationRequest;
//import com.ecommerce.notificationservice.service.NotificationService;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.boot.SpringApplication;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//public class Temp {
//
//    public static void main(String[] args) {
//
//        ConfigurableApplicationContext context =
//                SpringApplication.run(NotificationServiceApplication.class, args);
//
//        TemplateRepositoryPort templateRepository =
//                context.getBean(TemplateRepositoryPort.class);
//
//        NotificationService notificationService =context.getBean(NotificationService.class);
//        Optional<NotificationTemplate> template =
//                templateRepository.findByEventTypeAndChannel(
//                        "OTP_REQUEST",
//                        ChannelType.EMAIL
//                );
//        NotificationRequest notificationRequest = new NotificationRequest();
//        notificationRequest.setEventType("OTP_REQUEST");
//        notificationRequest.setChannelType(ChannelType.SMS);
//        notificationRequest.setRecipient("+918125968893");
//        Map<String ,String> map = new HashMap<>();
//        map.put("otp","210800");
//        notificationRequest.setParams(map);
//        notificationService.processNotification(notificationRequest);
//    }
//}
