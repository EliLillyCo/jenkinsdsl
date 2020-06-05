package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.pipeline.PipelineScopeMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class JenkinsScopeSpec extends CirrusDSLSpec {

  def "getName should return the name"() {
    given: "a pipeline scope"
    PipelineScopeMock scope = new PipelineScopeMock()

    when: "getName() is called"
    String name = scope.getName()

    then: "it should return the name"
    name == scope.name
  }

  def "name() should set the name"() {
    given: "a pipeline scope"
    PipelineScopeMock scope = new PipelineScopeMock()
    String newName = "New Name"

    when: "name() is called with the new name"
    scope.name(newName)

    then: "it should set the new name"
    scope.name == newName
  }

  def "withJenkins should run the supplied closure on the pipeline object"() {
    given: "a pipeline scope configured with pipeline and a closure containing withJenkins"
    PipelineScopeMock scope = new PipelineScopeMock()
    scope.ok = true
    JenkinsSim pipeline = createJenkinsSim()
    scope.jenkins = pipeline
    Closure<?> closure = {
      closureExecuted = true

      withJenkins {
        echo "Listing all files"
        sh "ls -lah"
      }
    }

    when: "the closure is executed on the scope delegate"
    DelegateFirstRunner.run scope, closure

    then: "the closure should be executed on the scope object"
    scope.closureExecuted

    and: "echo should be called on the pipeline object"
    pipeline.trace(SimFactory.echo("Listing all files"))

    and: "sh should be executed on the pipeline object"
    pipeline.trace(SimFactory.sh("ls -lah"))
  }

  def "getBuildStatus() should create a build status based on pipeline object"() {
    given: "a pipeline scope and a mock pipeline"
    PipelineScopeMock scope = new PipelineScopeMock()
    scope.jenkins = [currentBuild: []]

    when: "getBuildStatus is called on the pipeline scope object"
    BuildStatus status = scope.getBuildStatus()

    then: "it should return a status object"
    status
  }
}
