pipeline {
    agent { label 'bloomberg-terminal-node' }

    stages {
        stage('Environment Check') {
            steps {
                // Verify Maven and Java are visible to the agent
                bat 'java -version'
                bat 'mvn -version'
            }
        }
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Execute Delta1 Automation') {
            steps {
                // Use 'call' for mvn to ensure the script continues correctly in Windows
                bat 'call mvn clean test'
            }
        }
    }

    post {
        always {
            // Archive results even if the test fails
            junit '**/target/surefire-reports/*.xml'
            
            script {
                try {
                    // Force kill processes that lock the workspace
                    bat 'taskkill /F /IM WinAppDriver.exe /T || echo Already closed'
                    bat 'taskkill /F /IM node.exe /T || echo Appium already closed'
                    
                    // Clean workspace only if it's not locked
                    cleanWs(deleteDirs: true, disableDeferredWipeout: true)
                } catch (Exception e) {
                    echo "Cleanup skipped: Workspace is currently locked by another process."
                }
            }
        }
    }
}