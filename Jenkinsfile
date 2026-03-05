pipeline {
    // 1. Force execution on your specific Bloomberg Terminal machine
    agent { label 'bloomberg-terminal-node' }

    environment {
        // Ensure Jenkins knows where your tools are
        MAVEN_HOME = "C:\\apache-maven-3.x.x" 
        PATH = "${env.MAVEN_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Cleanup Environment') {
            steps {
                echo "Closing any hanging automation processes..."
                // Ensures a fresh start by killing old Appium/WinAppDriver sessions
                bat 'taskkill /F /IM WinAppDriver.exe /T || echo WinAppDriver not running'
                bat 'taskkill /F /IM node.exe /T || echo Appium not running'
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
                // Your code will now Attach to the manually opened Delta1 Window
                bat 'call mvn test'
            }
        }
    }

    post {
        always {
            // Archive Greeks/Graphs test results for the Jenkins dashboard
            junit '**/target/surefire-reports/*.xml'
            
            // Archive screenshots of graphs for visual debugging
            archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true
        }

        failure {
            // Instant notification with build details
            slackSend(
                color: 'danger', 
                message: "❌ Bloomberg Automation Failed! \nBuild: ${env.BUILD_NUMBER} \nURL: ${env.BUILD_URL}",
                channel: '#delta1-alerts'
            )
            // Optional: Upload the failure screenshot directly to Slack
            // slackUploadFile(filePath: 'target/screenshots/*.png', channel: '#delta1-alerts')
        }

        success {
            slackSend(
                color: 'good', 
                message: "✅ Bloomberg Delta1 Greeks validated successfully. Build #${env.BUILD_NUMBER}",
                channel: '#delta1-alerts'
            )
        }
    }
}