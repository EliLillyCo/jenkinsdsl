package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.pipeline.PipelineScopeMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class StashingShellStageSpec extends CirrusDSLSpec {

  def "a shell stage builder should correctly build the stage"() {
    given: "configurable fields for a shell stage"
    String name = "shell command"
    String stashName = "stash"
    String command = "ls -lah"
    Closure<?> when = {}
    Closure<?> postScript = {}

    when: "a shell builder builds the shell stage"
    StashingShellStage stage = StashingShellStage.Builder.create()
      .withName(name)
      .withStashName(stashName)
      .withCommand(command)
      .withWhen(when)
      .withPostScript(postScript)
      .build()

    then: "the shell stage should be built with supplied fields"
    stage.getName() == name
    stage.getStashName() == stashName
    stage.getCommand() == command
    stage.getWhen() == when
    stage.getPostScript() == postScript
    stage.getScript() != null
    stage.getDryScript() != null
  }

  def "a stashing shell stage should allow overriding script closure at runtime"() {
    given: "a shell stage with configured command and a closure"
    StashingShellStage stage = StashingShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    Closure<?> closure = {1 == 2}

    when: "script is invoked with custom closure"
    stage.script closure

    then: "it should override the default closure with the custom one"
    stage.getScript() == closure
  }

  def "a stashing shell stage should allow overriding dry run script closure at runtime"() {
    given: "a shell stage with configured command and a custome closure"
    StashingShellStage stage = StashingShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    Closure<?> closure = {1 == 1}

    when: "dryScript is invoked with custom closure"
    stage.dryScript closure

    then: "it should override the default closure"
    stage.getDryScript() == closure
  }

  def "a stashing shell stage should execute the supplied command on shell and stash the result"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new PipelineScopeMock()
    scope.pod = "pod"
    scope.ok = true
    scope.stages = [:]
    JenkinsSim pipeline = createJenkinsSim()
    scope.jenkins = pipeline

    String command = "ls -lah"
    String stashName = "stash"

    StashingShellStage stage = StashingShellStage.Builder.create()
      .withStashName(stashName)
      .withCommand(command)
      .build()

    String file = stashName + StashingShellStage.EXTENSION
    String adjustedCommand = "${command} 2>&1 | tee ${file}"

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should execute the shell command"
    pipeline.trace SimFactory.sh(adjustedCommand)

    then: "it should stash the result"
    pipeline.trace SimFactory.stash(name: stashName, includes: file)
  }

  def "a stashing shell stage should echo the supplied command during dry run"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new PipelineScopeMock()
    scope.ok = true
    scope.dryRun = true
    scope.pod = "pod"
    scope.stages = [:]

    JenkinsSim pipeline = createJenkinsSim()
    scope.jenkins = pipeline

    String command = "ls -lah"
    String stashName = "stash"

    StashingShellStage stage = StashingShellStage.Builder.create()
      .withStashName(stashName)
      .withCommand("ls -lah")
      .build()

    String file = stashName + StashingShellStage.EXTENSION
    String adjustedCommand = "${command} 2>&1 | tee ${file}"

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should echo the shell command"
    pipeline.trace SimFactory.echo(adjustedCommand)


    then: "it should echo the stash command"
    pipeline.trace SimFactory.echo("stash name: ${stashName}, includes: ${file}")
  }

  def "a stashing shell stage must have stash name configured"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new PipelineScopeMock()
    scope.ok = true
    scope.dryRun = true
    JenkinsSim pipeline = createJenkinsSim()
    scope.jenkins = pipeline

    String command = "ls -lah"

    StashingShellStage stage = StashingShellStage.Builder.create()
      .withCommand(command)
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "should throw an exception"
    thrown(CirrusPipelineException)
  }

  def "a stashing shell stage must have the command configured"() {
    given: "a scope configured with a pipeline and a shell stage configured with a stash name"
    Scope scope = new PipelineScopeMock()
    scope.ok = true
    scope.jenkins = createJenkinsSim()

    String stashName = "stash"

    StashingShellStage stage = StashingShellStage.Builder.create()
      .withStashName(stashName)
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "should throw an exception"
    thrown(CirrusPipelineException)
  }
}
