# Reactservice
**Reactservice** is a React based frontend microservice built as a repository of commonly used components.<br><br>
It also serves as a frontend for all of the other backend microservices of the MSINIT project.
<br><br><br>

## Features
- Contains prebuilt - commonly used components such as toolbar and confirmation popups.
- Integrates with all other MSINIT microservices
- Ready-to-run Docker environment
- Postman collection for testing and exploration
  <br><br><br>

## Technology Stack
- Javascript
- Vite
- React
- NPM
- Docker & Docker Compose
  <br><br><br>

## Getting Started

### Prerequisites
- Docker and Docker Compose installed
  <br><br>

### Running the Application
Clone the repository and start the service using Docker Compose:

```
git clone https://github.com/StrangeQuark/reactservice.git
cd reactservice
docker-compose up --build
```
<br>

### Environment Variables
The `.env` file is required to provide necessary configuration such as encryption secrets and database credentials. Default values are provided in `.env` file so the application can run out-of-the-box for testing.

⚠️ **Warning**: Do not deploy this application to production without properly changing your environment variables. The provided `.env` is not safe to use past local deployments!
<br><br>

## Testing
Unit tests are provided for pages and components.

```bash
npm ci
npm test
```
<br><br>

## Deployment
This project includes a `Jenkinsfile` for use in CI/CD pipelines. Jenkins must be configured with:

- Docker support
- Secrets or environment variables for configuration
- Access to any relevant private repositories, if needed
  <br><br>

## Optional: MSINIT Backend Integrations
Reactservice can integrate with all of the MSINIT backend services. Find a list of all the different services below.

- [Authservice GitHub Repository](https://github.com/StrangeQuark/authservice)
- [Emailservice GitHub Repository](https://github.com/StrangeQuark/emailservice)
- [Fileservice GitHub Repository](https://github.com/StrangeQuark/fileservice)
- [Vaultservice GitHub Repository](https://github.com/StrangeQuark/vaultservice)
<br><br>

## License
This project is licensed under the Apache License 2.0. See `LICENSE` for details.
<br><br>

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
