import com.cgi.et.jenkins.pipeline.StdMavenPipeline

import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Publish (Snapshot) :: Maven"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  configFileProvider([configFile(fileId: settings.pipelineParams.build.tool.settings, variable: "mavenSettingsXml")]) {
    settings.mavenSettingsXml = mavenSettingsXml

    StdMavenPipeline mavenPipeline = new StdMavenPipeline(this, settings, env)

    Closure mvnPipeFlow = {
      mavenPipeline.publishSnapshot()
    }

    mavenPipeline.pipelineFlow(mvnPipeFlow)
  }

  echo "##### END: [Pipeline Library] :: Publish (Snapshot) :: Maven"
}
