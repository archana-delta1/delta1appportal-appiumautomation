pipeline {
  agent { label 'windows-gui-agent' }
  environment {
    MAVEN_OPTS = '-Xmx1024m'
    ARTIFACT_DIR = "ci-artifacts/${env.BUILD_NUMBER}"
  }
  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '30'))
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    stage('Build') {
      steps {
        bat 'mvn -B -DskipTests clean package'
      }
    }
    stage('Run Tests') {
      steps {
        // wrapper ensures artifacts are copied to ARTIFACT_DIR
        bat 'ci\\run-tests.bat'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts artifacts: "${env.ARTIFACT_DIR}/**/*, target/surefire-reports/*.xml", fingerprint: true
        junit 'target/surefire-reports/*.xml'
      }
    }
  }
  post {
    always {
      // keep Extent report accessible in Jenkins UI
      publishHTML(target: [
        reportName: 'Extent Report',
        reportDir: "${env.ARTIFACT_DIR}",
        reportFiles: 'ExtentReport.html',
        keepAll: true
      ])
    }
    failure {
      echo "Build failed - artifacts and logs archived."
    }
  }
}