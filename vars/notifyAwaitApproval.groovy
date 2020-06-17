def call(options) {
  def rawApprovers = options.approvers
  def csvApproverUsernames = {
    switch (rawApprovers) {
      case String:
        // already csv
        return rawApprovers
      case Map:
        // keys are usernames and values are names
        return rawApprovers.keySet().join(',')
      case ArrayList:
        return rawApprovers.join(',')
      default:
        throw new Exception("Unexpeced approver type ${rawApprovers.class}!")
    }
  }()

  //def jobName = friendlyJobName()
  def jobName = 'deploy'
  node {
    // emailext needs to be inside a node block but do not want to take up a node while waiting for approval
    emailext body: "Build: <b>${jobName}</b><br>Build Number: <b>${env.BUILD_NUMBER}</b><br><br>Action is required to ${options.emailPrompt}  at:<br><b> ${env.JOB_URL}</b>",
        to: csvApproverUsernames,
        subject: "Action Required For Build ${jobName} (#${env.BUILD_NUMBER})"
  }

  return csvApproverUsernames

  /*milestone()
  input message: options.message,
        submitter: csvApproverUsernames
  milestone()*/
}
