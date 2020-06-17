import com.cgi.et.jenkins.pipeline.StdPipelineParser

import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  StdPipelineParser task = new StdPipelineParser(this, settings)

  Closure taskOverride = {
    task.stagesMap()
  }

  task.pipelineFlow(taskOverride)
}
