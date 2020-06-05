package com.lilly.cirrus.jenkinsdsl.git

import com.lilly.cirrus.jenkinsdsl.core.CirrusPipelineException
import com.lilly.cirrus.jenkinsdsl.core.DelegateFirstRunner

/**
 * Use this helper class to read the contents of a file on Github repo in the current branch.
 * The supplied file path should be relative to the root of the repository.
 */
class GithubFile implements Serializable {
  private static final long serialVersionUID = -6411912065301368465L
  def pipeline
  String repoBase
  String branchName
  String filePath
  Map<String, String> bindings = [:]

  static String mergePath(String path, String fileName) {
    if (!path || path == "" || path == "." || path == "./" || path == "/") return fileName

    return "${path}/${fileName}"
  }

  protected String getBranchName() {
    if (this.@branchName) return this.@branchName

    def env = pipeline.env

    def branchName = env.BRANCH_NAME
    if (env.CHANGE_BRANCH)
      branchName = env.CHANGE_BRANCH

    return branchName
  }

  protected String getRepoBase() {
    if (this.@repoBase) return this.@repoBase

    DelegateFirstRunner.run(pipeline) {
      String url = scm.userRemoteConfigs[0].url
      url = url.replace("https://github.com/", '')
      url = url.replace(".git", '')
      return url
    }
  }

  String read() {
    if (!filePath) throw new IOException("A file path relative to the root of repository must be supplied.")
    if (!pipeline) throw new CirrusPipelineException("The pipeline field must be supplied to a GithubFile.")

    DelegateFirstRunner.run(pipeline) {
      withCredentials([string(credentialsId: "github-secret-text", variable: "gitHubToken")]) {
        def curl = "curl -H \"Authorization: token ${gitHubToken}\"" +
          " -L https://raw.githubusercontent.com/${this.getRepoBase()}/${this.getBranchName()}/${this.getFilePath()}"
        String contents = sh(script: curl, returnStdout: true).trim()
        return transform(contents)
      }
    }
  }

  String transform(String contents) {
    bindings.each {key, value ->
      contents = contents.replaceAll(key, value)
    }
    return contents
  }
}
