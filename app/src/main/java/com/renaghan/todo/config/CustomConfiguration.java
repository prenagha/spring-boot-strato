package com.renaghan.todo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CustomConfigurationProperties.class)
class CustomConfiguration {}
