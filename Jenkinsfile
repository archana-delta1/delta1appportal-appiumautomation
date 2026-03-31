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
	stage('Execute Automated Test Suite') {           
		 steps {
                // 1. securely pull the secrets from the vault
                withCredentials([
                    string(credentialsId: 'DB_USERNAME_SECRET', variable: 'SECRET_USER'),
                    string(credentialsId: 'DB_PASSWORD_SECRET', variable: 'SECRET_PASS')
                ]) {
                    // 2. Create the config.properties file on the fly (Windows batch command)
                    bat """
                        echo db.host=127.0.0.1 > src\\test\\resources\\config.properties
                        echo db.port=1433 >> src\\test\\resources\\config.properties
                        echo db.name=OptionBlotter >> src\\test\\resources\\config.properties
                        echo db.user=%SECRET_USER% >> src\\test\\resources\\config.properties
                        echo db.password=%SECRET_PASS% >> src\\test\\resources\\config.properties
                    """
                    
                    // 3. Run the tests
                    bat 'mvn clean test-compile test'
                }
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
} 