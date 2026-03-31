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

        stage('Checkout Source Code') {
            steps {
                // Just grab the code, do NOT run Maven yet!
                checkout scm
            }
        }

        stage('Execute Automated Test Suite') {           
            steps {
                // 1. Securely pull the secrets from the vault
                withCredentials([
                    string(credentialsId: 'DB_USERNAME_SECRET', variable: 'SECRET_USER'),
                    string(credentialsId: 'DB_PASSWORD_SECRET', variable: 'SECRET_PASS')
                ]) {
                    // 2. Create the config.properties file on the fly
                    bat """
                        echo db.host=127.0.0.1 > src\\test\\resources\\config.properties
                        echo db.port=1433 >> src\\test\\resources\\config.properties
                        echo db.name=DeltaOne >> src\\test\\resources\\config.properties
                        echo db.user=%SECRET_USER% >> src\\test\\resources\\config.properties
                        echo db.password=%SECRET_PASS% >> src\\test\\resources\\config.properties
                    """
                    
                    // 3. Verify it was written (Debugging only, remove this later so it doesn't print passwords!)
                    bat 'type src\\test\\resources\\config.properties'
                    
                    // 4. NOW run the tests while the file exists
                    bat 'mvn clean test'
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
            
            // SECURITY CLEANUP: Delete the generated properties file
            bat 'del /f /q src\\test\\resources\\config.properties 2>nul || exit 0'

            // Cleanup ports again
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