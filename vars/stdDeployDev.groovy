import static com.cgi.et.jenkins.pipeline.StdPipeline.BUILD_TOOL_GRADLE
import static com.cgi.et.jenkins.pipeline.StdPipeline.BUILD_TOOL_MAVEN
import static com.cgi.et.jenkins.pipeline.StdPipeline.BUILD_TOOL_PYTHON
import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Deploy to DEV"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  switch (settings.pipelineParams.build.tool.name) {
    case BUILD_TOOL_MAVEN:
      stdDeployDevMaven { pipelineParams = settings.pipelineParams }
      break
    case BUILD_TOOL_GRADLE:
      // fall-through
    case BUILD_TOOL_PYTHON:
      // fall-through
    default:
      currentBuild.description = "${env.PIPELINE_CONFIG_FILE}: build.tool.name=\"${settings.pipelineParams.build.tool.name}\" not recognized (see documentation)"
      error "${currentBuild.description}"
  }

  echo "##### END: [Pipeline Library] :: Deploy to DEV"
}
