package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.git.GitHubEventType
import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.pipeline.PipelineShellMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class PipelineSpec extends CirrusDSLSpec {

  def "runShellStage should run the closure on the stage and then execute the corresponding stage"() {
    given: "A mock pipeline and shell command to execute"
    PipelineShellMock aPipeline = new PipelineShellMock()
    JenkinsSim pipeline = createJenkinsSim()
    aPipeline.jenkins = pipeline
    aPipeline.ok = true
    aPipeline.pod = "pod"
    aPipeline.stages = [:]

    Closure<?> stageBody = { stageBodyExecuted = true }
    String command = "ls -lah"

    when: "runShellStage is called with the command, and closure body"
    ShellStage stage = aPipeline.runShellStage("File List", command, stageBody)

    then: "the stage name if properly configured"
    stage.name == "File List"

    then: "the command is properly configured"
    stage.command == command

    then: "closure body is executed on the stage"
    stage.stageBodyExecuted

    then: "pipeline executes the shell script"
    pipeline.trace SimFactory.sh(command)
  }

  def "runStashingShellStage should run the closure on the stage and then execute the corresponding stage"() {
    given: "A mock pipeline, stash name, and shell command to execute"
    PipelineShellMock aPipeline = new PipelineShellMock()
    JenkinsSim pipeline = createJenkinsSim()
    aPipeline.jenkins = pipeline
    aPipeline.ok = true
    aPipeline.pod = "pod"
    aPipeline.stages = [:]

    Closure<?> stageBody = { stageBodyExecuted = true }
    String stashName = "fileList"
    String command = "ls -lah"
    String file = stashName + StashingShellStage.EXTENSION
    String adjustedCommand = "${command} 2>&1 | tee ${file}"

    when: "runStashingShellStage is called with the command, and closure body"
    StashingShellStage stage = aPipeline.runStashingShellStage("File List", stashName, command, stageBody)

    then: "the stage name is properly configured"
    stage.name == "File List"

    then: "the stash name is properly configured"
    stage.stashName == stashName

    then: "the command is properly configured"
    stage.command == command

    then: "closure body is executed on the stage"
    stage.stageBodyExecuted

    then: "pipeline executes the shell script"
    pipeline.anchor(SimFactory.sh(adjustedCommand))
      .next(SimFactory.stash(name: stashName, includes: file))
  }

  def "runStage should execute a new stage"() {
    given: "A mock pipeline and stage to execute"
    PipelineShellMock aPipeline = new PipelineShellMock()
    JenkinsSim pipeline = createJenkinsSim()

    aPipeline.jenkins = pipeline
    aPipeline.ok = true
    aPipeline.pod = "pod"
    aPipeline.stages = [:]

    when: "a new stage is defined in the pipeline"
    Stage stage = aPipeline.runStage("Test") {
      script {
        withJenkins {
          echo "This is a test!"
        }
      }
    }

    then: "pipeline executes the stage"
    pipeline.anchor(SimFactory.stage("Test"))
      .next(SimFactory.echo("This is a test!"))
  }

  def "calling when with falsy closure on a pipeline should set the ok property to be false"() {
    given: "a pipeline with ok set to true"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.ok = true

    when: "when is called on the pipeline with a falsy closure"
    aPipeline.when { 1 == 2 }

    then: "it should set the ok property of the pipeline to false"
    !aPipeline.ok
  }

  def "when should not evaluate its closure if the pipeline is not ok to execute"() {
    given: "a pipeline with ok set to true"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.ok = false

    when: "when is called on the pipeline with a truthy closure"
    aPipeline.when { 1 == 1 }

    then: "it should return and not evaluate the closure"
    !aPipeline.ok
  }

  def "anyOf should evaluate to true when any of the conditions are true"() {
    given: "a pipeline"
    PipelineShellMock aPipeline = new PipelineShellMock()

    when: "one of several boolean conditions is true"
    boolean truthy = true
    boolean falsy = false

    then: "calling anyOf with a truthy condition must always evaluate to true"
    aPipeline.anyOf(truthy)
    aPipeline.anyOf(truthy, truthy)
    aPipeline.anyOf(truthy, truthy, truthy)
    aPipeline.anyOf(falsy, truthy, truthy)
    aPipeline.anyOf(truthy, falsy, truthy)
    aPipeline.anyOf(falsy, falsy, truthy)

    and: "calling anyOf with all falsy conditions must always evaluate to false"
    !aPipeline.anyOf()
    !aPipeline.anyOf(falsy)
    !aPipeline.anyOf(falsy, falsy)
    !aPipeline.anyOf(falsy, falsy, falsy)
  }

  def "allOf should evaluate to true when all of the conditions are true"() {
    given: "a pipeline"
    PipelineShellMock aPipeline = new PipelineShellMock()

    when: "one of several boolean conditions is not true"
    boolean truthy = true
    boolean falsy = false

    then: "calling allOf with a falsy condition must always evaluate to false"
    !aPipeline.allOf(falsy)
    !aPipeline.allOf(falsy, truthy, truthy)
    !aPipeline.allOf(truthy, falsy, truthy)
    !aPipeline.allOf(falsy, falsy, truthy)
    !aPipeline.allOf(falsy, falsy)

    and: "calling allOf with all truthy conditions must always evaluate to true"
    aPipeline.allOf()
    aPipeline.allOf(truthy)
    aPipeline.allOf(truthy, truthy)
    aPipeline.allOf(truthy, truthy, truthy)
  }

  def "when a pr is created or a change is pushed to a PR, isPull should recognize it"() {
    given: "a PR github event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType(isPull: true)

    when: "isPull is run"
    boolean isPull = aPipeline.isPull()

    then: "it should detect the PR"
    isPull
  }

  def "when a regular branch is pushed, isPull should know it's not a PR change"() {
    given: "a github push event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType()

    when: "isPull is run"
    boolean isPull = aPipeline.isPull()

    then: "it should not detect a PR"
    !isPull
  }

  def "when a tag is pushed, isPull should know it's not a PR change"() {
    given: "a github release event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType()

    when: "isPull is run"
    boolean isPull = aPipeline.isPull()

    then: "it should not detect a PR"
    !isPull
  }

  def "when a pr is created on the master branch, isPullOnMaster should recognize it"() {
    given: "a github pull on master event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType(isPull: true, isPullOnMaster: true)

    when: "isPullOnMaster is run"
    boolean isPullOnMaster = aPipeline.isPullOnMaster()

    then: "it should detect a PR on master"
    isPullOnMaster
  }

  def "when a pr is created on a non-master branch, isPullOnMaster should know it's not a PR on master"() {
    given: "a github pull on non-master event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType(isPull: true, isPullOnMaster: false)

    when: "isPullOnMaster is run"
    boolean isPullOnMaster = aPipeline.isPullOnMaster()

    then: "it should not detect a PR on master"
    !isPullOnMaster
  }

  def "when a new release/tag is pushed, isRelease should recognize it"() {
    given: "a github release event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType(isRelease: true)

    when: "isRelease is run"
    boolean isRelease = aPipeline.isRelease()

    then: "isRelease should correctly identify the release"
    isRelease
  }

  def "when a release/tag is not pushed, isRelease should recognize that it's not a release"() {
    given: "a github pr event"
    PipelineShellMock aPipeline = new PipelineShellMock()
    aPipeline.gitHubEventType = new GitHubEventType(isPull: true)

    when: "isRelease is run"
    boolean isRelease = aPipeline.isRelease()

    then: "isRelease should not detect a release"
    !isRelease
  }

  def "when a branch is changed, isBranchChange should recognize it"() {
    given: "an environment with a branch change"
    PipelineShellMock aPipeline = new PipelineShellMock()
    def env = [BRANCH_NAME: "master"]
    aPipeline.env = env

    when: "isBranchChange is run"
    boolean isBranchChange = aPipeline.isBranchChange()

    then: "isBranch should correctly identify the branch has changed"
    isBranchChange
  }

  def "when its a tag change, isBranchChange should know that it's not a branch change"() {
    given: "an environment with a PR or a release"
    PipelineShellMock aPipeline = new PipelineShellMock()
    def env = [BRANCH_NAME: "1.0.0", TAG_NAME: "1.0.0"]
    aPipeline.env = env

    when: "isBranchChange is run"
    boolean isBranchChange = aPipeline.isBranchChange()

    then: "isBranch should correctly identify that the change is not a branch change"
    !isBranchChange
  }

  def "when it's a PR change, isBranchChange should know that it's not a branch change"() {
    given: "an environment with a PR"
    PipelineShellMock aPipeline = new PipelineShellMock()
    def env = [BRANCH_NAME: "PR-1", CHANGE_BRANCH: "feature/abc", CHANGE_TARGET: "master"]
    aPipeline.env = env

    when: "isBranchChange is run"
    boolean isBranchChange = aPipeline.isBranchChange()

    then: "isBranch should correctly identify that the change is not a branch change"
    !isBranchChange
  }



}
