# Use official WireMock image
FROM wiremock/wiremock:latest

# Set working directory
WORKDIR /home/wiremock

# Expose WireMock port
EXPOSE 8081

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Start WireMock with verbose logging
ENTRYPOINT ["/docker-entrypoint.sh", "--verbose"] 