pipeline {
  agent { label 'windows-gui-agent' }
  //agent any
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }
    stage('Prepare') {
      steps {
        // start Appium in background and redirect logs
        bat '"C:\\Program Files\\nodejs\\node.exe" "C:\\Users\\jenkins\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js" > appium.log 2>&1 &'
        // wait for Appium to be ready (simple loop)
        bat '''
          powershell -Command "
            $ready = $false; 
            for ($i=0; $i -lt 30 -and -not $ready; $i++) {
              try { $r = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:4723/wd/hub/status -TimeoutSec 2; if ($r.StatusCode -eq 200) { $ready = $true } }
              catch {}
              Start-Sleep -Seconds 2
            }
            if (-not $ready) { exit 1 }
          "
        '''
      }
    }
    stage('Run Tests') {
      steps {
        bat 'mvn test' // or your test runner command
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'appium.log', allowEmptyArchive: true
        }
      }
    }
    stage('Teardown') {
      steps {
        // stop Appium (kill by port or process name)
        bat 'for /f "tokens=5" %%a in (\'netstat -ano ^| findstr :4723\') do taskkill /PID %%a /F || echo "no appium process"'
      }
    }
  }
}
