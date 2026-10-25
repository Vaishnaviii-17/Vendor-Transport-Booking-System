
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
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

        stage('Test') {
            steps {
                withCredentials([
                    string(credentialsId: 'vendor-jwt-secret',
                           variable: 'JWT_SECRET')
                ]) {
                    bat 'mvn -B "-Dtest=*,!VendorTransportSeleniumTest" test'
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
        }

        success {
            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true
        }
    }
}
