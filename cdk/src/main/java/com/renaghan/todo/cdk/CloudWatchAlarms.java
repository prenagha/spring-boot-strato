package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.Network;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.AlarmProps;
import software.amazon.awscdk.services.cloudwatch.AlarmRule;
import software.amazon.awscdk.services.cloudwatch.AlarmState;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.CompositeAlarm;
import software.amazon.awscdk.services.cloudwatch.CompositeAlarmProps;
import software.amazon.awscdk.services.cloudwatch.CreateAlarmOptions;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.MetricProps;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.logs.FilterPattern;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.MetricFilter;
import software.amazon.awscdk.services.logs.MetricFilterProps;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;

class CloudWatchAlarms {

  CloudWatchAlarms(CDKApp app, Stack stack, Network network) {
    String loadBalancerName =
        Fn.split(":loadbalancer/", network.getLoadBalancer().getLoadBalancerArn(), 2).get(1);

    Alarm elbSlowResponseTimeAlarm =
        new Alarm(
            stack,
            "elbSlowResponseTimeAlarm",
            AlarmProps.builder()
                .alarmName("slow-api-response-alarm")
                .alarmDescription("Indicating potential problems with the Spring Boot Backend")
                .metric(
                    new Metric(
                        MetricProps.builder()
                            .namespace("AWS/ApplicationELB")
                            .metricName("TargetResponseTime")
                            .dimensionsMap(Map.of("LoadBalancer", loadBalancerName))
                            .region(app.awsEnv().getRegion())
                            .period(Duration.minutes(5))
                            .statistic("avg")
                            .build()))
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .evaluationPeriods(3)
                .threshold(2)
                .actionsEnabled(true)
                .build());

    Alarm elb5xxAlarm =
        new Alarm(
            stack,
            "elb5xxAlarm",
            AlarmProps.builder()
                .alarmName("5xx-backend-alarm")
                .alarmDescription(
                    "Alert on multiple HTTP 5xx ELB responses."
                        + "See the runbook for a diagnosis and mitigation hints: https://github.com/stratospheric-dev/stratospheric/blob/main/docs/runbooks/elb5xxAlarm.md")
                .metric(
                    new Metric(
                        MetricProps.builder()
                            .namespace("AWS/ApplicationELB")
                            .metricName("HTTPCode_ELB_5XX_Count")
                            .dimensionsMap(Map.of("LoadBalancer", loadBalancerName))
                            .region(app.awsEnv().getRegion())
                            .period(Duration.minutes(5))
                            .statistic("sum")
                            .build()))
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .evaluationPeriods(3)
                .datapointsToAlarm(3)
                .threshold(5)
                .actionsEnabled(false)
                .build());

    MetricFilter errorLogsMetricFilter =
        new MetricFilter(
            stack,
            "errorLogsMetricFilter",
            MetricFilterProps.builder()
                .metricName("backend-error-logs")
                .metricNamespace("stratospheric")
                .metricValue("1")
                .defaultValue(0)
                .logGroup(
                    LogGroup.fromLogGroupName(stack, "applicationLogGroup", app.appEnv() + "-logs"))
                .filterPattern(
                    FilterPattern.stringValue("$.level", "=", "ERROR")) // { $.level = "ERROR" }
                .build());

    Metric errorLogsMetric =
        errorLogsMetricFilter.metric(
            MetricOptions.builder()
                .period(Duration.minutes(5))
                .statistic("sum")
                .region(app.awsEnv().getRegion())
                .build());

    Alarm errorLogsAlarm =
        errorLogsMetric.createAlarm(
            stack,
            "errorLogsAlarm",
            CreateAlarmOptions.builder()
                .alarmName("backend-error-logs-alarm")
                .alarmDescription("Alert on multiple ERROR backend logs")
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .evaluationPeriods(3)
                .threshold(5)
                .actionsEnabled(false)
                .build());

    CompositeAlarm compositeAlarm =
        new CompositeAlarm(
            stack,
            "basicCompositeAlarm",
            CompositeAlarmProps.builder()
                .actionsEnabled(true)
                .compositeAlarmName("backend-api-failure")
                .alarmDescription("Showcasing a Composite Alarm")
                .alarmRule(
                    AlarmRule.allOf(
                        AlarmRule.fromAlarm(elb5xxAlarm, AlarmState.ALARM),
                        AlarmRule.fromAlarm(errorLogsAlarm, AlarmState.ALARM)))
                .build());

    Topic snsAlarmingTopic =
        new Topic(
            stack,
            "snsAlarmingTopic",
            TopicProps.builder()
                .topicName(app.appEnv() + "-alarming-topic")
                .displayName("SNS Topic to further route Amazon CloudWatch Alarms")
                .build());

    String e = "me@example.invalid";
    snsAlarmingTopic.addSubscription(EmailSubscription.Builder.create(e).build());

    elbSlowResponseTimeAlarm.addAlarmAction(new SnsAction(snsAlarmingTopic));
    compositeAlarm.addAlarmAction(new SnsAction(snsAlarmingTopic));
  }
}
