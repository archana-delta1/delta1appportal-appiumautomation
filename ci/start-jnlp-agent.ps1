param(
  [Parameter(Mandatory=$true)][string]$JenkinsUrl,
  [Parameter(Mandatory=$true)][string]$AgentName,
  [Parameter(Mandatory=$true)][string]$AgentSecret,
  [string]$AgentDir = "C:\ci-agent"
)

# Example Jenkins JNLP URL: https://jenkins.example.com/jnlpJars/agent.jar
$agentJarUrl = "$JenkinsUrl/jnlpJars/agent.jar"
$agentJarPath = Join-Path $AgentDir "agent.jar"

Write-Host "Downloading agent.jar from $agentJarUrl"
Invoke-WebRequest -Uri $agentJarUrl -OutFile $agentJarPath -UseBasicParsing

Write-Host "Starting JNLP agent interactively..."
# Keep this PowerShell window open; it will run the agent and connect to controller
Start-Process -FilePath "java" -ArgumentList "-jar `"$agentJarPath`" -jnlpUrl `"$JenkinsUrl/computer/$AgentName/jenkins-agent.jnlp`" -secret $AgentSecret -workDir `"$AgentDir\workspace`"" -NoNewWindow -Wait