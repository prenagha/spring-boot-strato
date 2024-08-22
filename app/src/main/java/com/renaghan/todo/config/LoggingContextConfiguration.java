package com.renaghan.todo.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
class LoggingContextConfiguration implements WebMvcConfigurer {

  private final ApplicationEventPublisher eventPublisher;

  public LoggingContextConfiguration(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LoggingContextInterceptor(eventPublisher));
  }
}
