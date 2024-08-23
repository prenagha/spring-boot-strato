package com.renaghan.todo.config;

import com.renaghan.todo.tracing.TracingEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.servlet.HandlerInterceptor;

class LoggingContextInterceptor implements HandlerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(LoggingContextInterceptor.class);
  private final MeterRegistry meterRegistry;
  private final ApplicationEventPublisher eventPublisher;

  public LoggingContextInterceptor(
      MeterRegistry meterRegistry, ApplicationEventPublisher eventPublisher) {
    this.meterRegistry = meterRegistry;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = getUserIdFromPrincipal(authentication.getPrincipal());
    // logback context so gets in cloudwatch json logging
    MDC.put("userId", userId);
    // event which is then async written to dynamodb breadcrumb table
    this.eventPublisher.publishEvent(new TracingEvent(this, request.getRequestURI(), userId));

    meterRegistry.counter("stratospheric.web.hits", Tags.of(request.getRequestURI())).increment();

    return true;
  }

  private String getUserIdFromPrincipal(Object principal) {
    if (principal instanceof String) {
      // anonymous users will have a String principal with value "anonymousUser"
      return principal.toString();
    }

    if (principal instanceof OidcUser) {
      try {
        OidcUser user = (OidcUser) principal;
        if (user.getPreferredUsername() != null) {
          return user.getPreferredUsername();
        } else if (user.getClaimAsString("name") != null) {
          return user.getClaimAsString("name");
        } else {
          logger.warn("could not extract userId from Principal");
          return "unknown";
        }
      } catch (Exception e) {
        logger.warn("could not extract userId from Principal", e);
      }
    }
    return "unknown";
  }

  @Override
  public void afterCompletion(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler,
      final Exception ex) {
    MDC.clear();
  }
}
