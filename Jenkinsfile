
pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
    }

    environment {
        CI = 'true'
        DOCKER_EXE = 'C:/Users/vaish/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe'
        IMAGE_NAME = 'vendor-transport-booking'
        CONTAINER_NAME = 'vendor-transport-app'
        DB_URL = 'jdbc:postgresql://host.docker.internal:5432/vendor_transport_db'
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

        stage('Docker Build') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    $docker = $env:DOCKER_EXE
                    $image = "$($env:IMAGE_NAME):$($env:BUILD_NUMBER)"

                    Write-Host "Building Docker image: $image"

                    & $docker build -t $image .

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image build failed."
                    }

                    Write-Host "Docker image built successfully."
                '''
            }
        }

        stage('Docker Deploy and Verify') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'vendor-db-username',
                        variable: 'DB_USERNAME'
                    ),
                    string(
                        credentialsId: 'vendor-db-password',
                        variable: 'DB_PASSWORD'
                    ),
                    string(
                        credentialsId: 'vendor-jwt-secret',
                        variable: 'JWT_SECRET'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        $docker = $env:DOCKER_EXE
                        $container = $env:CONTAINER_NAME
                        $backup = "$container-backup"
                        $image = "$($env:IMAGE_NAME):$($env:BUILD_NUMBER)"
                        $url = "http://localhost:8084/actuator/health"

                        $envFile = Join-Path $env:WORKSPACE ".docker-deploy.env"
                        $previousPreserved = $false
                        $newContainerCreated = $false
                        $deploymentHealthy = $false

                        function Run-Docker {
                            & $docker @args

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker command failed with exit code $LASTEXITCODE"
                            }
                        }

                        Write-Host "Checking existing Docker containers..."

                        $existing = & $docker ps -a `
                            --filter "name=^/$container$" `
                            --format "{{.Names}}"

                        if ($LASTEXITCODE -ne 0) {
                            throw "Unable to inspect existing application container."
                        }

                        $backupExists = & $docker ps -a `
                            --filter "name=^/$backup$" `
                            --format "{{.Names}}"

                        if ($LASTEXITCODE -ne 0) {
                            throw "Unable to inspect backup container."
                        }

                        if ($backupExists -eq $backup) {
                            throw "A backup container already exists. Resolve it before deploying."
                        }

                        try {
                            if ($existing -eq $container) {
                                Write-Host "Stopping existing application container..."

                                Run-Docker stop $container
                                Run-Docker rename $container $backup

                                $previousPreserved = $true

                                Write-Host "Previous container preserved as $backup"
                            }

                            Write-Host "Preparing deployment environment..."

                            $lines = @(
                                "DB_URL=$env:DB_URL"
                                "DB_USERNAME=$env:DB_USERNAME"
                                "DB_PASSWORD=$env:DB_PASSWORD"
                                "JWT_SECRET=$env:JWT_SECRET"
                            )

                            [System.IO.File]::WriteAllLines(
                                $envFile,
                                $lines,
                                [System.Text.UTF8Encoding]::new($false)
                            )

                            Write-Host "Deploying Docker image: $image"

                            Run-Docker run -d `
                                --name $container `
                                --restart unless-stopped `
                                -p 8084:8083 `
                                --env-file $envFile `
                                $image

                            $newContainerCreated = $true

                            Remove-Item $envFile -Force -ErrorAction SilentlyContinue

                            Write-Host "Waiting for application startup..."

                            for ($i = 1; $i -le 36; $i++) {
                                $running = & $docker inspect `
                                    --format "{{.State.Running}}" `
                                    $container

                                if ($LASTEXITCODE -ne 0 -or $running -ne "true") {
                                    throw "New Docker container stopped unexpectedly."
                                }

                                try {
                                    $response = Invoke-RestMethod `
                                        -Uri $url `
                                        -TimeoutSec 5

                                    if ($response.status -eq "UP") {
                                        $deploymentHealthy = $true
                                        break
                                    }
                                }
                                catch {
                                    Write-Host "Health check pending ($i/36)..."
                                }

                                Start-Sleep -Seconds 5
                            }

                            if (-not $deploymentHealthy) {
                                throw "Deployment health verification timed out."
                            }

                            Write-Host "Deployment successful."
                            Write-Host "Application health: UP"
                            Write-Host "Application URL: http://localhost:8084"

                            if ($previousPreserved) {
                                Write-Host "Removing previous backup container..."
                                Run-Docker rm -f $backup
                                $previousPreserved = $false
                            }
                        }
                        catch {
                            Write-Host "Deployment failed. Attempting rollback..."

                            if ($newContainerCreated) {
                                & $docker rm -f $container | Out-Null
                            }
                            else {
                                $partial = & $docker ps -a `
                                    --filter "name=^/$container$" `
                                    --format "{{.Names}}"

                                if ($partial -eq $container) {
                                    & $docker rm -f $container | Out-Null
                                }
                            }

                            if ($previousPreserved) {
                                Write-Host "Restoring previous application container..."

                                & $docker rename $backup $container

                                if ($LASTEXITCODE -eq 0) {
                                    & $docker start $container

                                    if ($LASTEXITCODE -eq 0) {
                                        Write-Host "Rollback completed."
                                    }
                                    else {
                                        Write-Host "WARNING: Previous container could not be started."
                                    }
                                }
                                else {
                                    Write-Host "WARNING: Previous container could not be renamed."
                                }
                            }

                            throw
                        }
                        finally {
                            Remove-Item $envFile `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }
                    '''
                }
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
