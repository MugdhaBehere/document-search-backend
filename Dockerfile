# Use an official Java runtime
FROM eclipse-temurin:21-jdk

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . .

# Build the project using Maven
RUN ./mvnw clean package -DskipTests

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/document-search-backend-0.0.1-SNAPSHOT.jar"]
