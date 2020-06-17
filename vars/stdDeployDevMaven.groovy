import com.cgi.et.jenkins.pipeline.StdMavenPipeline

import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Deploy to DEV :: Maven"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  StdMavenPipeline mavenPipeline = new StdMavenPipeline(this, settings, env)

  Closure mvnPipeFlow = {
    mavenPipeline.deployDev()
  }

  mavenPipeline.pipelineFlow(mvnPipeFlow)

  echo "##### END: [Pipeline Library] :: Deploy to DEV :: Maven"
}
