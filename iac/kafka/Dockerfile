FROM confluentinc/cp-kafka:7.3.2

# Environment variables
ENV KAFKA_HOME=/opt/kafka

# Copy custom configurations
COPY config/server.properties /etc/kafka/server.properties.template

# Create directories for data
RUN mkdir -p /var/lib/kafka/data && \
    chmod -R 777 /var/lib/kafka

# Health check
HEALTHCHECK --interval=5s --timeout=5s --retries=3 \
  CMD kafka-topics --bootstrap-server=localhost:9092 --list || exit 1

# Default command
CMD ["/etc/confluent/docker/run"] 