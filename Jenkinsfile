pipeline {
    agent { label 'bloomberg-terminal-node' } // The label of your manual-launch machine

    environment {
        MAVEN_HOME = "C:\\Program Files\\apache-maven-3.9.x" // Path to Maven on the machine
        PATH = "${env.MAVEN_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Fetch Code') {
            steps {
                // Pulls the latest code from your GitHub repo
                checkout scm
            }
        }

        stage('Build & Dependencies') {
            steps {
                // Compiles the Java code and downloads Maven dependencies
                bat 'mvn clean compile'
            }
        }

        stage('Execute Automation') {
            steps {
                script {
                    try {
                        // Runs the tests. Your code will "Attach" to the Delta1 window
                        bat 'mvn test'
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        echo "Tests failed: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            // Archive test results (JUnit/Surefire) so you can see them in Jenkins
            junit '**/target/surefire-reports/*.xml'
            
            // Optional: Cleanup workspace
            cleanWs()
        }
    }
}