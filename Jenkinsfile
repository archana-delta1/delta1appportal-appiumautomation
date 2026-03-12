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
                    taskkill /F /PID %%a /T
                )
                :: Safely kill drivers if they are stuck
                taskkill /F /IM WinAppDriver.exe /T /FI "STATUS eq RUNNING" 2>nul || set errorlevel=0
                exit 0
                """
            }
        }

        stage('Checkout & Build') {
            steps {
                checkout scm
                bat 'call mvn clean compile'
            }
        }

    post {
        always {
            // Only try to record results if they exist
            script {
                try {
                    junit '**/target/surefire-reports/*.xml'
                } catch (Exception e) {
                    echo "No test results found to record."
                }
            }
            bat 'for /f "tokens=5" %%a in (\'netstat -aon ^| findstr :4725\') do taskkill /F /PID %%a /T 2>nul || exit 0'
        }
        
        failure {
            echo "Pipeline Failed. Check console output for details."
            // Commenting out Slack until you configure the credentials in Jenkins UI
            // slackSend(color: 'danger', message: "Build Failed: ${env.BUILD_URL}")
        }
    }