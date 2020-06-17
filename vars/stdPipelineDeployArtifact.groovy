import static groovy.lang.Closure.DELEGATE_FIRST

def call(Closure body) {
  echo "##### BEGIN: [Pipeline Library] :: Pipeline :: Deploy :: Artifact"

  // get the settings from the pipeline script
  Map settings = [:]
  body.resolveStrategy = DELEGATE_FIRST
  body.delegate = settings
  body()

  echo("Deploying artifact [${settings.pipelineParams.deploy.artifact.artifactId}-${params.VERSION}] to [${params.DEPLOY_ENV}]")

  checkout(
      [
          $class           : "GitSCM",
          branches         : [[name: "master"]],
          userRemoteConfigs:
              [
                  [
                      credentialsId: "Gitlab",
                      url          : "${env.SCM_SERVER_BASE_URL}/cgi/jenkins-pipeline-library.git"
                  ]
              ]
      ]
  )

  String sshUser = settings.pipelineParams.deploy.users.get(0)
  for (int i = 0; i < settings.pipelineParams.deploy.environments.size(); i++) {
    if (settings.pipelineParams.deploy.environments.get(i) == "${params.DEPLOY_ENV}") {
      sshUser = settings.pipelineParams.deploy.users.get(i)
      break
    }
  }
  echo("Using SSH user [${sshUser}] with Ansible to deploy to [${params.DEPLOY_ENV}]")

  String mavenRepo = env.MAVEN_RELEASES_REPO
  String dockerRepo = env.DOCKER_RELEASES_REPO
  if (params.VERSION.contains(env.SNAPSHOT)) {
    mavenRepo = env.MAVEN_SNAPSHOTS_REPO
    dockerRepo = env.DOCKER_SNAPSHOTS_REPO
  }

  boolean runHealthcheck = false
  String deployParams = """
    "ansible_ssh_user": "${sshUser}",
    "maven_repo": "${mavenRepo}",
    "docker_repo": "${dockerRepo}",
    "group_id": "${settings.pipelineParams.deploy.artifact.groupId}",
    "et_environment": "${params.DEPLOY_ENV.toLowerCase()}",
    "artifact_id": "${settings.pipelineParams.deploy.artifact.artifactId}",
    "version": "${params.VERSION}"\
"""

  if (!settings.pipelineParams.deploy.healthcheck) {
    echo "${env.PIPELINE_CONFIG_FILE}: deploy.healthcheck is undefined"
    echo "WARNING: Health Check will be SKIPPED due to missing configuration in ${env.PIPELINE_CONFIG_FILE}"
  } else {
    runHealthcheck = true
    if (!settings.pipelineParams.deploy.healthcheck.url) {
      currentBuild.description = "${env.PIPELINE_CONFIG_FILE}: deploy.healthcheck.url is undefined (see documentation)"
      error "${currentBuild.description}"
    } else {
      deployParams += """,
    "health_url": "${settings.pipelineParams.deploy.healthcheck.url}"\
"""
      if (settings.pipelineParams.deploy.healthcheck.retry) {
        deployParams += """,
    "health_retry": ${settings.pipelineParams.deploy.healthcheck.retry}\
"""
      }
      if (settings.pipelineParams.deploy.healthcheck.retryInterval) {
        deployParams += """,
    "retry_interval": ${settings.pipelineParams.deploy.healthcheck.retryInterval}\
"""
      }
    }
  }
  deployParams += """,
    "run_healthcheck": ${runHealthcheck}
"""

  if (settings.pipelineParams.deploy) {
//    if (!settings.pipelineParams.deploy.target) {
//      currentBuild.description = "${env.PIPELINE_CONFIG_FILE}: deploy.target is undefined (see documentation)"
//      error "${currentBuild.description}"
//    }
    switch (settings.pipelineParams.deploy.pattern) {
      case env.DEPLOY_PATTERN_KUBERNETES:
        echo "Deploying To Kubernetes..."

        withKubeConfig([credentialsId: 'KubeSecret', serverUrl: 'https://F0EC612C1DE48D07A101C2A8638E9976.yl4.us-west-2.eks.amazonaws.com']) {
          sh """
            /var/jenkins_home/kubectl apply -f ./pipelines/deployment/kube-deploy/ -n=admin
          """
          echo "Application Deployed using Kubernetes Pattern"
        }
        break
      case env.DEPLOY_PATTERN_CONTAINER:
        echo "Deploying To Azure Containers..."

        withCredentials([usernamePassword(credentialsId: 'ACR', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

          String image = "snapshot-registry.pipeline.et-apps.net/cgi/example-spring-boot-app:1.0.0-SNAPSHOT"
          sh '/usr/bin/az container create \
                --resource-group immutable-pipeline-demo \
                --name springboot-app \
                --image immutablepipelineregistry.azurecr.io/cgi/example-spring-boot-app:1.0.0-SNAPSHOT \
                --dns-name-label springboot-app1 \
                --ports 8080 \
                --registry-username $USERNAME \
                --registry-password $PASSWORD'
        }
        break
      case env.DEPLOY_PATTERN_DOCKER_COMPOSE:
        sh """
          ansible-playbook \
          deploy_docker_compose_services.yml \
          --inventory inventories/${params.DEPLOY_ENV.toLowerCase()} \
          --limit ${settings.pipelineParams.deploy.target.toLowerCase()} \
          --extra-vars '{ ${deployParams} }'
        """
        break
      case env.DEPLOY_PATTERN_DOCKER_COMPOSE_SSH:
        sh """
          ansible-playbook \
          deploy_docker_compose_services_ssh.yml \
          --inventory inventories/${params.DEPLOY_ENV.toLowerCase()} \
          --limit ${settings.pipelineParams.deploy.target.toLowerCase()} \
          --extra-vars '{ ${deployParams} }'
        """
        break
      default:
        currentBuild.description = "${env.PIPELINE_CONFIG_FILE}: deploy.pattern=\"${settings.pipelineParams.deploy.pattern}\" is unrecognized (see documentation)"
        error "${currentBuild.description}"
    }
  } else {
    currentBuild.description = "${env.PIPELINE_CONFIG_FILE}: deploy is undefined (see documentation)"
    error "${currentBuild.description}"
  }

  echo "##### END: [Pipeline Library] :: Pipeline :: Deploy :: Artifact"
}
