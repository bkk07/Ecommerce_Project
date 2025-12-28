package com.ecommerce.notificationservice.service.factory;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.service.strategy.NotificationChannelStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChannelStrategyFactory {
    private final Map<ChannelType, NotificationChannelStrategy> strategyMap;

    public ChannelStrategyFactory(List<NotificationChannelStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotificationChannelStrategy::getSupportedType, Function.identity()));
    }

    public NotificationChannelStrategy getStrategy(ChannelType type) {
        return strategyMap.get(type);
    }
}