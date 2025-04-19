FROM eclipse-temurin:17-jre-alpine

# Environment variables
ENV ACTIVEMQ_VERSION 5.18.2
ENV ACTIVEMQ_HOME /opt/activemq
ENV ACTIVEMQ_USER activemq
ENV ACTIVEMQ_GROUP activemq
ENV ACTIVEMQ_UID 1000
ENV ACTIVEMQ_GID 1000

# Install dependencies
RUN apk add --no-cache curl bash

# Download and extract ActiveMQ
RUN set -ex; \
    curl -L https://archive.apache.org/dist/activemq/${ACTIVEMQ_VERSION}/apache-activemq-${ACTIVEMQ_VERSION}-bin.tar.gz -o activemq.tar.gz; \
    mkdir -p /opt; \
    tar -xzf activemq.tar.gz -C /opt; \
    rm activemq.tar.gz; \
    ln -s /opt/apache-activemq-${ACTIVEMQ_VERSION} ${ACTIVEMQ_HOME}; \
    addgroup -g ${ACTIVEMQ_GID} -S ${ACTIVEMQ_GROUP}; \
    adduser -u ${ACTIVEMQ_UID} -S ${ACTIVEMQ_USER} -G ${ACTIVEMQ_GROUP} -h ${ACTIVEMQ_HOME}; \
    chown -R ${ACTIVEMQ_USER}:${ACTIVEMQ_GROUP} ${ACTIVEMQ_HOME} /opt/apache-activemq-${ACTIVEMQ_VERSION}

# Add custom configuration
COPY conf/* ${ACTIVEMQ_HOME}/conf/

# Configure JMX
ENV ACTIVEMQ_JMX_PORT 1099

# Expose ActiveMQ ports
# 8161 - Web console
# 61616 - JMS
# 5672 - AMQP
# 61613 - STOMP
# 1883 - MQTT
EXPOSE 8161 61616 5672 61613 1883 1099

# Change to activemq user
USER ${ACTIVEMQ_USER}

# Set working directory
WORKDIR ${ACTIVEMQ_HOME}

# Start ActiveMQ
CMD ["bin/activemq", "console"] 