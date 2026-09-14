# Gatewayservice
**Gatewayservice** serves as an API gateway to the other MSINIT microservices.
<br><br><br>

## Features
- Spring Cloud Gateway as a reverse proxy for other microservices
- Integrates with all MSINIT microservices
- Ready-to-run Docker environment
- Postman collection for testing and exploration
  <br><br><br>

## Technology Stack
- Java 17+
- Spring Cloud Gateway
- Docker & Docker Compose
  <br><br><br>

## Getting Started

### Prerequisites
- Docker and Docker Compose installed
- Java 17+ (for development or test execution outside Docker)
  <br><br>

### Running the Application
Clone the repository and start the service using Docker Compose:

```
git clone https://github.com/StrangeQuark/gatewayservice.git
cd gatewayservice
docker-compose up --build
```
<br>

### Environment Variables
The `.env` file is required to provide necessary configuration such as encryption secrets and database credentials. Default values are provided in `.env` file so the application can run out-of-the-box for testing.

⚠️ **Warning**: Do not deploy this application to production without properly changing your environment variables. The provided `.env` is not safe to use past local deployments!
<br><br>

## API Documentation
A Postman collection is included in the root of the project:

- `Gatewayservice.postman_collection.json`

This collection provides all endpoints for the other MSINIT microservices:
- Authservice
- Emailservice
- Fileservice
- Vaultservice
- Telemetryservice
- Reactservice
  <br><br>

## Deployment
This project includes a `Jenkinsfile` for use in CI/CD pipelines. Jenkins must be configured with:

- Docker support
- Secrets or environment variables for configuration
- Access to any relevant private repositories, if needed
  <br><br>

## Optional: MSINIT Integrations
Gatewayservice integrates with all MSINIT services. Find a list of all the different services below.

- [Authservice GitHub Repository](https://github.com/StrangeQuark/authservice)
- [Emailservice GitHub Repository](https://github.com/StrangeQuark/emailservice)
- [Fileservice GitHub Repository](https://github.com/StrangeQuark/fileservice)
- [Vaultservice GitHub Repository](https://github.com/StrangeQuark/vaultservice)
- [Telemetryservice GitHub Repository](https://github.com/StrangeQuark/telemetryService)
- [Reactservice GitHub Repository](https://github.com/StrangeQuark/reactservice)
<br><br>

## License
This project is licensed under the Apache License 2.0. See `LICENSE` for details.
<br><br>

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
