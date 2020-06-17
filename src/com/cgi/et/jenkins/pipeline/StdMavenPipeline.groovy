package com.cgi.et.jenkins.pipeline


import org.jenkinsci.plugins.workflow.cps.EnvActionImpl

class StdMavenPipeline extends StdPipeline {

  Object pipe
  Map settings
  EnvActionImpl env

  StdMavenPipeline(Object pipe, Map settings, EnvActionImpl env) {
    super(pipe, settings)
    this.pipe = pipe
    this.settings = settings
    this.env = env
  }

  def buildSnapshot() {
    def flags = ""
    if (settings.pipelineParams.stages && settings.pipelineParams.stages.build && settings.pipelineParams.stages.build.flags) {
      flags = settings.pipelineParams.stages.build.flags
    }
    pipe.sh "mvn clean install -Dmaven.test.skip=true --update-snapshots --settings ${settings.mavenSettingsXml} ${flags}"
  }

  def buildRelease() {
    def flags = ""
    if (settings.pipelineParams.stages && settings.pipelineParams.stages.build && settings.pipelineParams.stages.build.flags) {
      flags = settings.pipelineParams.stages.build.flags
    }
    pipe.sh "mvn clean release:clean release:prepare release:perform -Dmaven.test.skip=true --batch-mode --update-snapshots --settings ${settings.mavenSettingsXml} ${flags}"
  }

  def testUnit() {
    def flags = ""
    if (settings.pipelineParams.stages && settings.pipelineParams.stages.testUnit && settings.pipelineParams.stages.testUnit.flags) {
      flags = settings.pipelineParams.stages.testUnit.flags
    }
    pipe.sh "mvn test --update-snapshots --settings ${settings.mavenSettingsXml} ${flags}"
  }

  def testFunctional() {
    def flags = ""
    if (settings.pipelineParams.stages && settings.pipelineParams.stages.testFunctional && settings.pipelineParams.stages.testFunctional.flags) {
      flags = settings.pipelineParams.stages.testFunctional.flags
    }
    pipe.sh "mvn verify --update-snapshots --settings ${settings.mavenSettingsXml} ${flags}"
  }

  def scanPr() {
    // set git repo value to: "<github-org>/<repo-name>"
    def gitRepo = env.CHANGE_URL.replaceFirst("^[^\\/]+\\/\\/[^\\/]+\\/([^\\/]+\\/[^\\/]+).*", "\$1")
    def prId = env.CHANGE_ID
    def prSourceBranch = env.CHANGE_BRANCH
    def prTargetBranch = env.CHANGE_TARGET
    pipe.sh "mvn dependency-check:aggregate sonar:sonar -Dsonar.pullrequest.github.repository=${gitRepo} -Dsonar.pullrequest.key=${prId} -Dsonar.pullrequest.branch=${prSourceBranch} -Dsonar.pullrequest.base=${prTargetBranch} --settings ${settings.mavenSettingsXml}"
  }

  def scanSnapshot() {
    if (env.BRANCH_NAME == env.BRANCH_MASTER) {
      pipe.sh "mvn dependency-check:aggregate sonar:sonar --settings ${settings.mavenSettingsXml}"
    } else {
      pipe.sh "mvn dependency-check:aggregate sonar:sonar -Dsonar.branch.name=${env.BRANCH_NAME} --settings ${settings.mavenSettingsXml}"
    }
  }

  def scanRelease() {
    def releaseVersion = pipe.sh script: "mvn help:evaluate -Dexpression=project.version --quiet -DforceStdout --file ./target/checkout/pom.xml", returnStdout: true
    pipe.sh "mvn dependency-check:aggregate sonar:sonar -Dsonar.branch.name=tag-${releaseVersion} --file ./target/checkout/pom.xml --settings ${settings.mavenSettingsXml}"
  }

  def publishSnapshot() {
    pipe.sh "mvn deploy -Dmaven.test.skip=true --settings ${settings.mavenSettingsXml}"
  }

  def publishRelease() {
    pipe.sh "mvn deploy --activate-profiles create-release --file ./target/checkout/pom.xml --settings ${settings.mavenSettingsXml}"
  }

  def clean() {
    pipe.sh "mvn clean"
  }

  def deployDev() {
    def deployPipelineJobName = env.JOB_NAME.replace("-" + env.SNAPSHOT_TYPE.toLowerCase() + "/", "-" + env.DEPLOY_TYPE.toLowerCase() + "/")
    def artifactVersion = pipe.sh script: "mvn help:evaluate -Dexpression=project.version --quiet -DforceStdout", returnStdout: true
    pipe.echo "Deploying version [${artifactVersion}] to environment: ${env.ENV_DEV}"
    pipe.build job: deployPipelineJobName,
        parameters: [
            pipe.string(name: "VERSION", value: artifactVersion),
            pipe.string(name: "DEPLOY_ENV", value: env.ENV_DEV)
        ],
        wait: false
  }

}
