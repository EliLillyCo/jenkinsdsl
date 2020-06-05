package com.lilly.cirrus.jenkinsdsl.core


class Pipeline extends EntryPoint {
  private static final long serialVersionUID = 1L

  protected Stage runShellStage(String name, String command, Closure<?> body = null) {
    ShellStage stage = ShellStage.Builder.create()
      .withCommand(command)
      .withName(name)
      .withBody(body)
      .build()

    this.execute(stage)
  }

  protected Stage runStashingShellStage(String name, String stashName, String command, Closure<?> body = null) {
    StashingShellStage stage = StashingShellStage.Builder.create()
      .withStashName(stashName)
      .withCommand(command)
      .withName(name)
      .withBody(body)
      .build()

    this.execute(stage)
  }

  Stage runStage(String name, Closure<?> closure) {
    Stage stage = Stage.Builder.create()
      .withName(name)
      .withBody(closure)
      .build()

    this.execute(stage)
  }

  Stage abortPreviousBuilds(Closure<?> body) {
    Stage stage = BuildAbortionStage.Builder.create()
      .withName("Abort Previous Builds")
      .withBody(body)
      .build()

    this.execute(stage)
  }


  Stage runNoActionStage(String name = "Skipping Build", boolean includeInComment = true, Closure<?> body = null) {
    StageConfig stageConfig = StageConfig.create(name: name, includeInComment: includeInComment)

    Stage stage = Stage.Builder.create()
      .withStageConfig(stageConfig)
      .withPostScript { stageConfig.warn = true }
      .withBody(body)
      .build()

    execute stage
  }


  protected def when(Closure<?> closure) {
    if (!this.ok)
      return
    boolean check = DelegateFirstRunner.run this, closure
    this.ok = check
  }

  boolean anyOf(boolean ... conditions) {
    for (boolean cond : conditions) {
      if (cond) {
        return true
      }
    }
    return false
  }

  boolean allOf(boolean ... conditions) {
    for (boolean cond : conditions) {
      if (!cond) {
        return false
      }
    }
    return true
  }

  boolean isPull() {
    this.gitHubEventType.isPull
  }

  boolean isPullOnMaster() {
    this.gitHubEventType.isPullOnMaster
  }

  boolean isPullOnDevelop() {
    this.gitHubEventType.isPullOnDevelop
  }

  boolean isRelease() {
    this.gitHubEventType.isRelease
  }

  boolean isPreRelease() {
    this.gitHubEventType.isPreRelease
  }

  boolean isPushToMaster() {
    this.gitHubEventType.isPushToMaster
  }

  boolean isPullRequestMergeToMaster() {
    this.gitHubEventType.isPullRequestMergeToMaster
  }

  boolean isPullRequestMergeToDevelop() {
    this.gitHubEventType.isPullRequestMergeToDevelop
  }

  boolean isBranchChange() {
    return this.env.TAG_NAME == null && this.env.CHANGE_TARGET == null
  }

  boolean isFeaturePull() {
    String changeBranch = this.env.CHANGE_BRANCH
    this.gitHubEventType.isPull && changeBranch != 'develop' && changeBranch != 'master'
  }
}
