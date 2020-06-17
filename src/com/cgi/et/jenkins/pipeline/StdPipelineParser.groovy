package com.cgi.et.jenkins.pipeline

import com.cgi.et.jenkins.pipeline.StdLogging
import com.cgi.et.jenkins.pipeline.StdPipeline

class StdPipelineParser extends StdPipeline {

  Object pipe
  Map settings
  def tool

  StdPipelineParser(Object pipe, Map settings) {
    super(pipe, settings)
    this.pipe = pipe
    this.settings = settings
  }

  Map stagesMap() {
    // list of stages which are allowed to be overridden
    def stages = ["build", "test", "scan"]
    Map stageTaskMap = [:]
    def customTask

    tool = this.isDefined("build.tool.name")

    switch (tool) {
      case BUILD_TOOL_MAVEN:
        Map defaultStageTaskMap = ["build": "stdBuildSnapshotMaven", "testUnit": "stdTestUnitMaven", "scan": "stdScanSnapshotMaven"]
        for (stage in stages) {
          customTask = this.isDefined("stages.${stage}.customTask")
          if (customTask) {
            stageTaskMap.put("${stage}", customTask)
          } else {
            //stageTaskMap.put("${stage}", "${defaultStageTaskMap.get(stage)}")
            stageTaskMap.put("${stage}", "${defaultStageTaskMap.get(stage)}")
          }
        }
    }
    StdLogging.logParameters(pipe, stageTaskMap, "Stages")
    return stageTaskMap
  }

}
