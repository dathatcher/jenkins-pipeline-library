import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Pipeline :: Deploy :: Approval"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  echo "Verify that only a RELEASE artifact can be deployed above ${env.ENV_DEV}"
  if (params.VERSION.contains(env.SNAPSHOT)) {
    currentBuild.description = "You cannot deploy a SNAPSHOT artifact to ${env.ENV_QA} and above"
    error "You cannot deploy a SNAPSHOT artifact to ${env.ENV_QA} and above"
  }

  echo "Verify that RELEASE artifact has been successfully deployed to prior environments (not applicable to ${env.ENV_QA} environment)"
  if (params.DEPLOY_ENV != env.ENV_QA) {
    int index = settings.pipelineParams.deploy.environments.findIndexOf { it == "${params.DEPLOY_ENV}" }
    String previousEnv = settings.pipelineParams.deploy.environments.get(index - 1)

    echo "Retrieve artifact metadata via API call"
    String repoApiUrl = "${env.ARTIFACT_REPO_BASE_URL}/api/storage/${env.MAVEN_RELEASES_REPO}/${settings.pipelineParams.deploy.artifact.artifactPath}?properties"
    artifactProps = httpRequest authentication: env.ARTIFACT_REPO_USER, httpMode: "GET", url: repoApiUrl
    artifactPropsJson = readJSON text: artifactProps.content
    if (!artifactPropsJson.properties."${previousEnv}") {
      currentBuild.description = "Failed prerequisite: artifact [${settings.pipelineParams.deploy.artifact.artifactId}] was never deployed to [${previousEnv}]"
      error "Failed prerequisite: artifact [${settings.pipelineParams.deploy.artifact.artifactId}::${params.VERSION}] was never deployed to [${previousEnv}]"
    }
  }

  String approvalGroup = ""
  switch (params.DEPLOY_ENV) {
    case env.ENV_QA:
      approvalGroup = env.APPROVAL_GROUP_QA
      break
    case env.ENV_STAGE:
      approvalGroup = env.APPROVAL_GROUP_STAGE
      break
    case env.ENV_PROD:
      approvalGroup = env.APPROVAL_GROUP_PROD
      break
    default:
      currentBuild.description = "Deploy environment [${params.DEPLOY_ENV}] not recognized"
      error "Deploy environment [${params.DEPLOY_ENV}] not recognized"
  }
  echo "Get approval for deployment from someone in [${approvalGroup}] (not applicable to ${env.ENV_DEV} environment)"
  if (settings.pipelineParams.notification.teams) {
    office365ConnectorSend color: env.BLUE,
        message: "Go to build to manually approve deployment",
        status: env.STATUS_BLOCKED,
        webhookUrl: settings.pipelineParams.notification.teams.channel.failure
  }
  timeout(time: 20, unit: "MINUTES") {
    input message: "Deploy [${settings.pipelineParams.deploy.artifact.artifactId}-${params.VERSION}] to [${params.DEPLOY_ENV}]?",
        submitter: "${approvalGroup}"
  }

  echo "##### END: [Pipeline Library] :: Pipeline :: Deploy :: Approval"
}
