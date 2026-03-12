pipeline {
    agent { label 'bloomberg-terminal-node' }

    stages {
        stage('Cleanup Environment') {
            steps {
                echo "Cleaning up port 4725..."
                bat """
                @echo off
                :: Only kill the process specifically listening on 4725
                for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4725') do (
                    taskkill /F /PID %%a /T 2>nul
                )
                :: Safely kill drivers if they are stuck
                taskkill /F /IM WinAppDriver.exe /T 2>nul
                exit 0
                """
            }
        }

        stage('Checkout, Build & Test') {
            steps {
                checkout scm
                bat 'mvn clean test'
            }
        }
    }

    post {
        always {
            script {
                try {
                    junit '**/target/surefire-reports/*.xml'
                } catch (Exception e) {
                    echo "No test results found to record."
                }
            }
            bat """
            @echo off
            for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4725') do taskkill /F /PID %%a /T 2>nul
            exit 0
            """
        }
        
        failure {
            echo "Pipeline Failed. Check console output for details."
            // slackSend(color: 'danger', message: "Build Failed: ${env.BUILD_URL}")
        }
    }
} // <--- Make sure you include this final closing brace!