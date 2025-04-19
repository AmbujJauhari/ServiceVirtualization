#!/bin/bash

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
cub kafka-ready -b kafka:9092 1 30

# Create topics
echo "Creating topics..."
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic virtualization.requests --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic virtualization.responses --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic virtualization.errors --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic virtualization.events --partitions 3 --replication-factor 1

# List topics
echo "Listing created topics:"
kafka-topics --bootstrap-server kafka:9092 --list

echo "Topic setup completed!" 