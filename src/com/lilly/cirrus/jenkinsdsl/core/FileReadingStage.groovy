package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.git.GithubFile

abstract class FileReadingStage extends Stage {
  protected String path
  protected String fileName

  void path(String path) {
    this.@path = path
  }

  String getPath() {
    return this.@path
  }

  void fileName(String fileName) {
    this.@fileName = fileName
  }

  String getFileName() {
    return this.@fileName
  }

  @Override
  Closure<?> getDryScript() {
    if (this.@dryScript) return this.@dryScript

    return { this.readRepoFile() }
  }

  @Override
  Closure<?> getScript() {
    if (this.@script) return this.@script

    return { this.readRepoFile() }
  }

  void readRepoFile() {
    String filePath = GithubFile.mergePath(this.@path, this.@fileName)

    withJenkins {
      if (fileExists(filePath)) readFileLocally()
      else downloadAndReadFile(filePath)
    }
  }

  void downloadAndReadFile(String filePath) {
    withJenkins {
      GithubFile gitHubFile = new GithubFile(pipeline: delegate, filePath: filePath)
      def contents = gitHubFile.read()

      String path = this.@path
      if (path == "" || path == "." || path == "./") path = null

      if (path) {
        sh "mkdir -p ${path}"
        dir(path) {
          writeFile file: this.@fileName, text: contents
        }
      }
      else writeFile file: this.@fileName, text: contents
    }

    readFileLocally()
  }

  abstract void readFileLocally()
}
