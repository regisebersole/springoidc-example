# Spring Boot REST API with OIDC Authentication

A Spring Boot REST API application with OIDC authentication support and React TypeScript frontend.

## Features

- Spring Boot 3.x with embedded Tomcat server
- OIDC authentication with Google and other providers
- JWT session management with timeout policies
- RESTful API endpoints with proper HTTP methods
- Comprehensive error handling and validation
- API documentation with Swagger/OpenAPI
- Health checks and monitoring with Actuator
- React TypeScript frontend with PKCE authentication flow

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 8.5 or higher (or use the included Gradle wrapper)
- Node.js 18 or higher (for frontend)

### Running the Application

1. Clone the repository
2. Set environment variables:
   ```bash
   export OIDC_CLIENT_ID=your-client-id
   export OIDC_CLIENT_SECRET=your-client-secret
   export JWT_SECRET=your-jwt-secret
   ```
3. Run the Spring Boot application:
   ```bash
   ./gradlew bootRun
   ```
4. The API will be available at `http://localhost:8080`

### API Documentation

Once the application is running, you can access the API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Health Checks

Health check endpoints are available at:
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

## Project Structure

```
src/
├── main/
│   ├── java/com/example/springbootrestapi/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── exception/       # Custom exceptions
│   │   ├── model/          # Data models and DTOs
│   │   ├── security/       # Security components
│   │   └── service/        # Business logic services
│   └── resources/
│       └── application.yml  # Application configuration
└── test/                   # Test classes
```

## Configuration

The application can be configured using environment variables or application.yml:

- `OIDC_CLIENT_ID`: OIDC client ID
- `OIDC_CLIENT_SECRET`: OIDC client secret
- `OIDC_INTROSPECTION_URI`: OIDC token introspection endpoint
- `JWT_SECRET`: Secret key for JWT signing
- `FRONTEND_URL`: Frontend application URL for CORS

## Development

### Running Tests

```bash
./gradlew test
```

### Building the Application

```bash
./gradlew build
```

The built JAR file will be available in the `build/libs/` directory.

### Other Useful Gradle Commands

```bash
# Clean build artifacts
./gradlew clean

# Run the application in development mode
./gradlew bootRun

# Generate Gradle wrapper (if needed)
./gradlew wrapper
```