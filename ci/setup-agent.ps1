# Run as Administrator
param(
  [string]$JavaVersion = "17",
  [string]$MavenVersion = "3.9.6"  # optional; choco will pick latest if omitted
)

Write-Host "Starting agent setup..."

# Install Chocolatey if missing
if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
  Write-Host "Installing Chocolatey..."
  Set-ExecutionPolicy Bypass -Scope Process -Force
  [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
  Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
} else {
  Write-Host "Chocolatey already installed."
}

# Install Java 17 (Temurin), Maven, Git
choco install -y temurin17 maven git

# Ensure environment variables are available in current session
$env:JAVA_HOME = (Get-ChildItem 'HKLM:\SOFTWARE\JavaSoft\JDK' -ErrorAction SilentlyContinue | Select-Object -First 1).PSChildName
if (-not $env:JAVA_HOME) {
  # fallback: try temurin path
  $possible = "C:\Program Files\Eclipse Adoptium\jdk-17*"
  $jdk = Get-ChildItem $possible -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
Write-Host "JAVA_HOME = $env:JAVA_HOME"

# Create folders for CI artifacts
$base = "C:\ci-agent"
New-Item -ItemType Directory -Path $base -Force | Out-Null
New-Item -ItemType Directory -Path "$base\workspace" -Force | Out-Null
New-Item -ItemType Directory -Path "$base\artifacts" -Force | Out-Null
New-Item -ItemType Directory -Path "$base\logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$base\screenshots" -Force | Out-Null

Write-Host "Agent setup complete. Please configure Jenkins agent and run start-jnlp-agent.ps1 interactively."