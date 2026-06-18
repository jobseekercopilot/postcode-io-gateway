# Postcode IO Gateway

A Spring Boot gateway service for the UK postcodes.io API, providing location-based functionality for job seeker applications.

## Features

- **Postcode Lookup**: Retrieve location data (latitude, longitude, region, etc.) for UK postcodes
- **Outcode Lookup**: Query postcode outcodes for geographic information
- **Spring Boot 3.2**: Built on the latest Spring Boot framework with Java 17
- **WebFlux Support**: Reactive programming model for improved performance
- **Actuator Endpoints**: Health checks and monitoring capabilities
- **Docker Ready**: Containerized deployment support

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring WebFlux** (reactive web framework)
- **Spring Boot Actuator** (monitoring)
- **Lombok** (boilerplate reduction)
- **Maven** (build tool)
- **Docker** (containerization)

## Project Structure

```
src/main/java/com/jobseekercopilot/postcodeiogateway/
├── PostcodeIoGatewayApplication.java  # Application entry point
├── client/
│   └── PostcodeIoApiClient.java       # HTTP client for postcodes.io API
├── controller/
│   └── PostcodeController.java        # REST API endpoints
├── model/
│   └── PostcodeLocation.java          # Data models
└── service/
    └── PostcodeService.java           # Business logic layer

src/test/java/com/jobseekercopilot/postcodeiogateway/
├── PostcodeControllerIntegrationTest.java
└── service/
    └── PostcodeServiceTest.java
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (optional, for containerized deployment)

### Building the Application

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Running with Docker

```bash
docker build -t postcode-io-gateway .
docker run -p 8080:8080 postcode-io-gateway
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Postcode Lookup
```
GET /api/postcodes/{postcode}
```
Returns location data for a given UK postcode.

### Outcode Lookup
```
GET /api/outcodes/{outcode}
```
Returns geographic information for a given outcode.

## Configuration

Key configuration properties in `src/main/resources/application.properties`:

- `server.port`: Application port (default: 8080)
- Postcodes.io API base URL configuration

## Testing

Run the test suite:

```bash
mvn test
```

## License

This project is part of the Job Seeker Copilot ecosystem.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request