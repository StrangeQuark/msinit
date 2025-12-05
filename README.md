# MSINIT
**MSINIT** is an open source backend for the MSINIT download page. It works in conjunction with the [MSINIT frontend](https://github.com/StrangeQuark/msinit-frontend) to serve the website hosted at [https://msinit.com](https://msinit.com)
<br><br><br>

## Technology Stack
- NodeJS
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
git clone https://github.com/StrangeQuark/msinit.git
cd msinit
docker-compose up --build
```
<br>

## Deployment
This project includes a `Jenkinsfile` for use in CI/CD pipelines. Jenkins must be configured with:

- Docker support
<br><br>

## License
This project is licensed under the GNU General Public License. See `LICENSE.md` for details.
<br><br>

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
