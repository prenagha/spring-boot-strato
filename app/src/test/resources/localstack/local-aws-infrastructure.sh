#!/bin/sh

awslocal sqs create-queue --queue-name dev-todo-app-todo-sharing-queue

awslocal ses verify-email-identity --email-address noreply@stratospheric.dev
awslocal ses verify-email-identity --email-address info@stratospheric.dev
awslocal ses verify-email-identity --email-address tom@stratospheric.dev
awslocal ses verify-email-identity --email-address bjoern@stratospheric.dev
awslocal ses verify-email-identity --email-address philip@stratospheric.dev

awslocal dynamodb create-table \
    --table-name dev-todo-app-breadcrumb \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10 \
