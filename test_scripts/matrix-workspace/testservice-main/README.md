# Testservice
**Testservice** is an end-to-end testing solution for all the services in the MSINIT stack.
<br><br><br>

## Features
- Full API testing coverage for all services within MSINIT
- UI testing for the Reactservice
- Ready-to-run Docker environment
- Jenkins configuration for running tests remotely
  <br><br><br>

## Technology Stack
- Java 17+
- Docker & Docker Compose
- Jenkins
- Playwright
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
git clone https://github.com/StrangeQuark/testservice.git
cd testservice
docker-compose up --build
```
<br>

### Environment Variables
The `.env` file is required to provide necessary configuration with Authservice. Default values are provided in `.env` file so the application can run out-of-the-box for testing.

⚠️ **Warning**: Do not deploy this application to production without properly changing your environment variables. The provided `.env` is not safe to use past local deployments!
<br><br>

## Deployment
This project includes a `Jenkinsfile` for use in CI/CD pipelines. Jenkins must be configured with:

- Docker support
- Secrets or environment variables for configuration
- Access to any relevant private repositories, if needed
  <br><br>

## Optional: MSINIT Integration
This service is meant to be used in conjunction with the other services within the MSINIT stack.

It is fully compatible with each service, and is meant to be initialized via the MSINIT.com webpage
<br><br>

## License
This project is licensed under the Apache License 2.0. See `LICENSE` for details.
<br><br>

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
