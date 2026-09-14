pipeline {
    agent { label 'linux-agent' }

    environment {
        VAULT_URL = credentials('VAULT_URL')
        CICD_TOKEN = credentials('CICD_TOKEN')
        VAULTSERVICE_ENABLED = credentials('VAULTSERVICE_ENABLED')
    }

    stages {
        stage("Retrieve Env Vars") {
            steps {
                script {
                    if(VAULTSERVICE_ENABLED == "true") {
                        def response = httpRequest(
                            url: VAULT_URL + '/api/vault/cicd',
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            requestBody: '{"serviceName":"testservice","environmentName":"e3"}',
                            customHeaders: [
                                [name: 'X-CICD-TOKEN', value: CICD_TOKEN, maskValue: true]
                            ],
                            validResponseCodes: '200'
                        )

                        writeFile file: 'testservice.env', text: response.content
                        echo "Environment variables written to testservice.env"
                    }
                }
            }
        }
        stage("Deploy & Test") {
            steps {
                script {
                    try {
                        sh "docker compose --env-file testservice.env up --build"
                    } catch (ex) {
                        echo "Unexpected failure: ${ex.getMessage()}"
                        sh "docker compose down"
                        error("Deployment crashed.")
                    } finally {
                        // Ensure containers are cleaned up
                        sh "docker compose down"
                    }
                }
            }
        }

        stage("Publish HTML Report") {
            steps {
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'test-results',
                    reportFiles: 'report.html',
                    reportName: 'Test Report'
                ])
            }
        }
    }

    post {
        always {
            sh "rm -f testservice.env"
            echo "Cleaned up testservice.env"
            sh "docker compose down -v"
        }
    }
}
