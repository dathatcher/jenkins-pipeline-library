import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Pass Quality :: Maven"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  // sleep because Jenkins does not react quickly enough to the SonarQube webhook
  // see: https://community.sonarsource.com/t/intermittent-waitforqualitygate-timeout-webhook-received-ignored/9878/3
  sleep(20) // this is actually seconds (not milliseconds)
  timeout(time: 10, unit: "MINUTES") {
    // re-use taskId previously collected by withSonarQubeEnv()
    def qualityGate = waitForQualityGate()
    if (qualityGate.status != "OK") {
      currentBuild.description = "Pipeline aborted due to quality gate failure: ${qualityGate.status}"
      error "Pipeline aborted due to quality gate failure: ${qualityGate.status}"
    }
  }

  echo "##### END: [Pipeline Library] :: Pass Quality :: Maven"
}
