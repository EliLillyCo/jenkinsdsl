package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class ShellStageSpec extends CirrusDSLSpec {

  def "a shell stage builder should correctly build the stage"() {
    given: "configurable fields for a shell stage"
    String command = "ls -lah"
    String name = "shell command"
    Closure<?> when = {}
    Closure<?> postScript = {}

    when: "a shell builder builds the shell stage"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand(command)
      .withName(name)
      .withWhen(when)
      .withPostScript(postScript)
      .build()

    then: "the shell stage should be built with supplied fields"
    stage.getName() == name
    stage.getCommand() == command
    stage.getWhen() == when
    stage.getPostScript() == postScript
    stage.getScript() != null
    stage.getDryScript() != null
  }

  def "a shell stage should allow overriding script closure at runtime"() {
    given: "a shell stage with configured command and a closure"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    Closure<?> closure = {1 == 1}

    when: "script is invoked with custom closure"
    stage.script closure

    then: "it should override the default closure with the new closure"
    stage.getScript() == closure
  }

  def "a shell stage should allow overriding dry run script closure at runtime"() {
    given: "a shell stage with configured command and a closure"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    Closure<?> closure = {1 == 1}

    when: "dryScript is invoked with custom closure"
    stage.dryScript closure

    then: "it should override the default closure with the new closure"
    stage.getDryScript() == closure
  }

  def "a shell stage should execute the supplied command on shell"() {
    given: "a scope configured with a jenkins and a shell stage configured with a command"
    Scope scope = new Scope()
    JenkinsSim jenkins = createJenkinsSim()
    setupScope(scope, jenkins)

    String command = "ls -lah"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should execute the shell command"
    jenkins.trace SimFactory.sh(command)
  }

  def "a shell stage should echo the supplied command during dry run"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new Scope()
    JenkinsSim jenkins = createJenkinsSim()
    setupScope(scope, jenkins)
    scope.dryRun = true

    String command = "ls -lah"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should execute the shell command"
    jenkins.trace SimFactory.echo(command)
  }
}
