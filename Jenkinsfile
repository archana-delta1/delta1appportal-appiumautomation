pipeline {
    agent { label 'bloomberg-terminal-node' }

    stages {
        stage('Cleanup Environment') {
            steps {
                echo "Cleaning up background processes..."
                bat """
           		 @echo off
           		 for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4725') do taskkill /F /PID %%a /T || echo Port 4725 already free
           		 taskkill /F /IM WinAppDriver.exe /T /FI "STATUS eq RUNNING" || exit 0
           		 taskkill /F /IM node.exe /T /FI "STATUS eq RUNNING" || exit 0
      			  """
            }
        }

        stage('Checkout & Build') {
            steps {
                checkout scm
                bat 'call mvn clean compile'
            }
        }

        stage('Execute Delta1 Automation') {
            steps {
                // Ensure your Delta1 Window is open before this starts!
                bat 'call mvn test'
            }
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
        }
        
        failure {
            echo "Pipeline Failed. Check console output for details."
            // Commenting out Slack until you configure the credentials in Jenkins UI
            // slackSend(color: 'danger', message: "Build Failed: ${env.BUILD_URL}")
        }
    }
}