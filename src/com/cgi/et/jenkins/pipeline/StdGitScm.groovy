package com.cgi.et.jenkins.pipeline

class StdGitScm implements Serializable {

  def pipe

  StdGitScm(pipe) {
    this.pipe = pipe
  }

  def checkoutRemoteGit(String repoUrl, String branch, String credentials) {
    pipe.checkout([$class                           : "GitSCM",
                   branches                         : [[name: branch]],
                   doGenerateSubmoduleConfigurations: false,
                   extensions                       : [[$class: "CleanCheckout"]],
                   submoduleCfg                     : [],
                   userRemoteConfigs                : [[credentialsId: credentials, url: repoUrl]]
    ])
  }

}
