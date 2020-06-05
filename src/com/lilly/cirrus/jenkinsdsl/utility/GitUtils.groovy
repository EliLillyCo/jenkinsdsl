package com.lilly.cirrus.jenkinsdsl.utility

class GitUtils {
  static String getRepoNameFromURL(String gitURL) {
    // https://stackoverflow.com/questions/45684941/how-to-get-repo-name-in-jenkins-pipeline
    return gitURL.tokenize('/').last().split("\\.")[0]
  }
}
