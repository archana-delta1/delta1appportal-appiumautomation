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
        withCredentials([usernamePassword(credentialsId: 'YOUR_CREDENTIAL_ID', passwordVariable: 'SECRET_PASS', usernameVariable: 'SECRET_USER')]) {
            script {
                // 1. Construct the file content natively in Groovy
                def configContent = """db.host=127.0.0.1
db.port=1433
db.name=DeltaOne
db.user=${SECRET_USER}
db.password=${SECRET_PASS}"""
                
                // 2. Write the file directly using Jenkins native step
                writeFile file: 'src/test/resources/config.properties', text: configContent
            }
            
            // 3. Run your tests (replace 'mvn test' with your actual test execution command)
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