package com.renaghan.todo.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
class LoggingContextConfiguration implements WebMvcConfigurer {

  private final MeterRegistry meterRegistry;
  private final ApplicationEventPublisher eventPublisher;

  public LoggingContextConfiguration(MeterRegistry meterRegistry, ApplicationEventPublisher eventPublisher) {
    this.meterRegistry= meterRegistry;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LoggingContextInterceptor(meterRegistry, eventPublisher));
  }
}
