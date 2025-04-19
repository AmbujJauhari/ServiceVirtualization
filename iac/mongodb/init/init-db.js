// MongoDB Initialization Script for Service Virtualization

// Initialize main database
db = db.getSiblingDB('service_virtualization');

// Create collections
print("Creating collections...");

// REST stubs collection
db.createCollection('rest_stubs');
db.rest_stubs.createIndex({ "userId": 1 });
db.rest_stubs.createIndex({ "status": 1 });

// SOAP stubs collection
db.createCollection('soap_stubs');
db.soap_stubs.createIndex({ "userId": 1 });
db.soap_stubs.createIndex({ "status": 1 });

// Kafka stubs collection
db.createCollection('kafka_stubs');
db.kafka_stubs.createIndex({ "userId": 1 });
db.kafka_stubs.createIndex({ "status": 1 });
db.kafka_stubs.createIndex({ "topicName": 1 });

// ActiveMQ stubs collection
db.createCollection('activemq_stubs');
db.activemq_stubs.createIndex({ "userId": 1 });
db.activemq_stubs.createIndex({ "status": 1 });
db.activemq_stubs.createIndex({ "destinationName": 1, "destinationType": 1 });
db.activemq_stubs.createIndex({ "priority": -1 });

// File stubs collection
db.createCollection('file_stubs');
db.file_stubs.createIndex({ "userId": 1 });
db.file_stubs.createIndex({ "status": 1 });

// TIBCO stubs collection
db.createCollection('tibco_stubs');
db.tibco_stubs.createIndex({ "userId": 1 });
db.tibco_stubs.createIndex({ "status": 1 });
db.tibco_stubs.createIndex({ "destinationName": 1 });

// IBM MQ stubs collection
db.createCollection('ibmmq_stubs');
db.ibmmq_stubs.createIndex({ "userId": 1 });
db.ibmmq_stubs.createIndex({ "status": 1 });
db.ibmmq_stubs.createIndex({ "queueManager": 1, "queueName": 1 });

// Users collection
db.createCollection('users');
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "username": 1 }, { unique: true });

// Insert an admin user
print("Creating admin user...");
db.users.insertOne({
  username: "admin",
  email: "admin@example.com",
  password: "$2a$10$hCWOWYLrkMCAqkqXvMcBK.rLW3Qz97aCji0nDa.hqH68BX7iWRXCi", // hashed "password"
  role: "ADMIN",
  createdAt: new Date(),
  updatedAt: new Date()
});

print("Database initialization completed successfully!");

// Add statistics collection
db.createCollection('usage_statistics');
db.usage_statistics.createIndex({ "timestamp": 1 });
db.usage_statistics.createIndex({ "userId": 1 });
db.usage_statistics.createIndex({ "stubType": 1 }); 