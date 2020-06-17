import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Pipeline :: Deploy :: Metadata"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  echo "Update artifact metadata via API call"
  String repoApiUrl = "${env.ARTIFACT_REPO_BASE_URL}/api/metadata/${env.MAVEN_RELEASES_REPO}/${settings.pipelineParams.deploy.artifact.artifactPath}"
  String artifactPropsUpdate = "{\"props\": {\"${params.DEPLOY_ENV}\": \"${env.BUILD_ID}\"}}"
  httpRequest authentication: env.ARTIFACT_REPO_USER, contentType: "APPLICATION_JSON", httpMode: "PATCH", requestBody: artifactPropsUpdate, url: repoApiUrl

  echo "##### END: [Pipeline Library] :: Pipeline :: Deploy :: Metadata"
}
