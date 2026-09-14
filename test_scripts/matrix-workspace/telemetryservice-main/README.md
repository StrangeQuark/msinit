# Telemetryservice
**Telemetryservice** is a secure microservice for processing analytics.
<br><br><br>

## Features
- Analytics engine for ingesting and processing telemetry data
- Supports synchronous API calls and asynchronous capabilities via Apache Kafka
- Optional AES-256 encryption for all telemetry metadata
- Optional JWT-based user authentication via external Authservice
- Ready-to-run Docker environment
- Postman collection for testing and exploration
  <br><br><br>

## Technology Stack
- Java 17+
- Spring Boot
- MongoDB
- Docker & Docker Compose
- JPA (Hibernate)
- JUnit 5
  <br><br><br>

## Getting Started

### Prerequisites
- Docker and Docker Compose installed
- Java 17+ (for development or test execution outside Docker)
  <br><br>

### Running the Application
Clone the repository and start the service using Docker Compose:

```
git clone https://github.com/StrangeQuark/telemetryservice.git
cd telemetryservice
docker-compose up --build
```
<br>

### Environment Variables
The `.env` file is required to provide necessary configuration such as encryption secrets and database credentials. Default values are provided in `.env` file so the application can run out-of-the-box for testing.

⚠️ **Warning**: Do not deploy this application to production without properly changing your environment variables. The provided `.env` is not safe to use past local deployments!
<br><br>

## API Documentation
A Postman collection is included in the root of the project:

- `Telemetryservice.postman_collection.json`
  <br><br>

## Testing
Unit tests are provided for all repository and service-layer logic.
<br><br>

## Deployment
This project includes a `Jenkinsfile` for use in CI/CD pipelines. Jenkins must be configured with:

- Docker support
- Secrets or environment variables for configuration
- Access to any relevant private repositories, if needed
  <br><br>

## Optional: MSINIT Integration
Telemetryservice can integrate with all other microservice within the MSINIT project.

For more information, see the following repositories:
- [Authservice GitHub Repository](https://github.com/StrangeQuark/authservice)
- [Emailservice GitHub Repository](https://github.com/StrangeQuark/emailservice)
- [Fileservice GitHub Repository](https://github.com/StrangeQuark/fileservice)
- [Vaultservice GitHub Repository](https://github.com/StrangeQuark/vaultservice)
- [Reactservice GitHub Repository](https://github.com/StrangeQuark/reactservice)
  <br><br>

## License
This project is licensed under the Apache License 2.0. See `LICENSE` for details.
<br><br>

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
