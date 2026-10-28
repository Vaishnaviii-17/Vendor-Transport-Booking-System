
# Task 11 - Docker Containerization
# Vendor Transport Booking System

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the executable Spring Boot JAR
COPY target/vendor-transport-booking-0.0.1-SNAPSHOT.jar app.jar

# Application runs on port 8083
EXPOSE 8083

# Start Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
