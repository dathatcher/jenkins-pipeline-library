package com.cgi.et.jenkins.pipeline

class StdUtils implements Serializable {

  def pipe

  StdUtils(pipe) {
    this.pipe = pipe
  }

  String getCurrentTask(Map stageTasksMap, String stageName) {
    def currTask = stageTasksMap.get("${stageName}")
    return currTask
  }

  Map getMapDiffs(Map sourceMap, Map destMap) {
    def diffKeys = sourceMap - destMap
    return diffKeys
  }

}
