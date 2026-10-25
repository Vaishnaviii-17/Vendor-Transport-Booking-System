
pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
    }

    environment {
        CI = 'true'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B -DskipTests compile'
            }
        }

        stage('Backend Tests') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'vendor-jwt-secret',
                        variable: 'JWT_SECRET'
                    )
                ]) {
                    bat 'mvn -B "-Dtest=*,!VendorTransportSeleniumTest" test'
                }
            }
        }

        stage('Check Application') {
            steps {
                powershell '''
                    $url = "http://localhost:8083"
                    $ready = $false

                    for ($i = 1; $i -le 12; $i++) {
                        try {
                            $response = Invoke-WebRequest `
                                -Uri $url `
                                -UseBasicParsing `
                                -TimeoutSec 5

                            if ($response.StatusCode -ge 200 -and
                                $response.StatusCode -lt 400) {
                                $ready = $true
                                break
                            }
                        }
                        catch {
                            Write-Host "Waiting for application..."
                        }

                        Start-Sleep -Seconds 5
                    }

                    if (-not $ready) {
                        throw "Application is not responding at $url"
                    }

                    Write-Host "Application is ready."
                '''
            }
        }

        stage('Selenium Tests') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'vendor-jwt-secret',
                        variable: 'JWT_SECRET'
                    )
                ]) {
                    bat 'mvn -B "-Dtest=VendorTransportSeleniumTest" "-Dselenium.headless=true" test'
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B -DskipTests package'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: 'target/surefire-reports/*.xml'

            archiveArtifacts(
                artifacts: 'target/selenium-screenshots/**/*.png',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        success {
            archiveArtifacts(
                artifacts: 'target/*.jar',
                fingerprint: true
            )
        }
    }
}
