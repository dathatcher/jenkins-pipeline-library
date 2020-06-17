package com.cgi.et.jenkins.pipeline

class StdPipeline implements Serializable {

  public static final String BUILD_TOOL_MAVEN = "maven"
  public static final String BUILD_TOOL_GRADLE = "gradle"
  public static final String BUILD_TOOL_PYTHON = "python"

  public static final String PIPELINE_YAML = "pipeline.yml"

  Object pipe
  Map settings = [:]

  def slaveNode = "master"

  StdPipeline(Object pipe, Map settings) {
    this.pipe = pipe
    this.settings = settings
  }

  def pipelineFlow(Closure pipeFlow) {
    def releaseConfig = pipe.readYaml file: PIPELINE_YAML
    StdUtils mapDiffs = new StdUtils(pipe)
    Map changes = mapDiffs.getMapDiffs(settings.pipelineParams, releaseConfig)

    if (changes) {
      StdLogging.logParameters(pipe, changes, "Additional Settings")
    }

    //StdGitScm git = new StdGitScm(this.pipe)
    //git.checkoutRemoteGit(settings.pipelineParams.project.projectUrl, settings.pipelineParams.project.branch, settings.pipelineParams.project.credentials)

    pipe.withEnv(this.getBuildEnv()) {
      pipeFlow.call()
    }
  }

  def getBuildEnv() {
    List<String> buildEnv = []

    def javaHome = pipe.tool name: "${settings.pipelineParams.build.language.name}-${settings.pipelineParams.build.language.version}"

    // language Setup
    if (javaHome != null) {
      buildEnv.add("JAVA_HOME=${javaHome}")
      buildEnv.add("PATH+JDK=${javaHome}/bin")
    }

    def mvnHome = pipe.tool name: "${settings.pipelineParams.build.tool.name}-${settings.pipelineParams.build.tool.version}"
    // Java build tool setup
    if (mvnHome != null) {
      buildEnv.add("PATH+MAVEN=${mvnHome}/bin")
    }

    buildEnv
  }

  String isDefined(String nestedMapKey) {
    def map = this.settings.pipelineParams
    if (map) {
      def mapkey = nestedMapKey.split(/\./)
      for (int c = 0; c < mapkey.length; c++) {
        map = map.get(mapkey[c])
        if (!map) {
          return null
        }
      }
      return map
    }
  }

}
