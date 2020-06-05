package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.utility.GitUtils

abstract class JenkinsScope extends Scope {
  private static final long serialVersionUID = 1L
  protected String name = this.getClass().simpleName

  String getName() {
    return this.name
  }

  /**
   * Sets the name of the block
   * @param name
   */
  void name(String name) {
    this.name = name
  }

  /**
   * Delegates all of the method calls and field acces to the jenkins object.
   * @param closure A groovy code block.
   * @return Any return value returned by the Groovy code block.
   */
  protected <T> T withJenkins(Closure<T> closure) {
    if (!this.ok) return null
    DelegateFirstRunner.run this.jenkins, closure
  }

  /**
   * Returns the build status as an object based on the current build status of the Jenkins pipeline.
   */
  BuildStatus getBuildStatus() {
    BuildStatus.fromPipeline(this.jenkins)
  }

  String getRepoUrl() {
    this.jenkins.scm.userRemoteConfigs[0].url
  }

  /**
   * Returns the repo name based on the Jenkins scm object config
   */
  String getRepoName() {
    GitUtils.getRepoNameFromURL(this.getRepoUrl())
  }
}
