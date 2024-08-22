package com.renaghan.todo.tracing;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
public class TraceDao {

  private final DynamoDbTemplate dynamoDbTemplate;

  public TraceDao(DynamoDbTemplate dynamoDbTemplate) {
    this.dynamoDbTemplate = dynamoDbTemplate;
  }

  @Async
  @EventListener(TracingEvent.class)
  public void storeTracingEvent(TracingEvent tracingEvent) {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setId(UUID.randomUUID().toString());
    breadcrumb.setUri(tracingEvent.getUri());
    breadcrumb.setUsername(tracingEvent.getUsername());
    breadcrumb.setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

    dynamoDbTemplate.save(breadcrumb);
  }

  public List<Breadcrumb> findAllEventsForUser(String username) {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setUsername(username);

    return dynamoDbTemplate
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(breadcrumb.getId()).build()))
                .build(),
            Breadcrumb.class)
        .items()
        .stream()
        .toList();
  }

  public List<Breadcrumb> findUserTraceForLastTwoWeeks(String username) {
    Instant twoWeeksAgo =
        ZonedDateTime.now().minusWeeks(2).toInstant().truncatedTo(ChronoUnit.SECONDS);

    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setUsername(username);

    return dynamoDbTemplate
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(breadcrumb.getId()).build()))
                .filterExpression(
                    Expression.builder()
                        .expression("timestamp > :twoWeeksAgo")
                        .putExpressionValue(
                            ":twoWeeksAgo",
                            AttributeValue.builder().s(twoWeeksAgo.toString()).build())
                        .build())
                .build(),
            Breadcrumb.class)
        .items()
        .stream()
        .toList();
  }
}
