package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.block.BlockContainerMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory
import hudson.model.Result
import jenkins.model.CauseOfInterruption
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

class ContainerBlockSpec extends CirrusDSLSpec {
  def "setup and teearDown should assign the respective closures"() {
    given: "a docker block and two closures"
    ContainerBlock block = new BlockContainerMock()
    Closure<?> setup = {1 == 1}
    Closure<?> tearDown = {2 == 1}

    when: "setup and teardown are called with closures"
    block.runSetup setup
    block.runTearDown tearDown

    then: "the corresponding fields are setup properly"
    block.setupClosure == setup
    block.tearDownClosure == tearDown
  }

  String getTestString(int length) {
    StringBuilder builder = new StringBuilder()
    for(int i = 0; i < length; ++i) {
      builder.append('A')
    }
    return builder.toString()
  }


  def "runPod should execute the pod"() {
    given: "Container block and a closure for running in a pod"
    ContainerBlock block = new BlockContainerMock()
    block.ok = true


    and: "a pipeline configured with variable bindings for credential ids"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])
    pipeline.withEnv([JOB_NAME: this.getTestString(55)])
    block.jenkins = pipeline

    and: "docker secret is created"
    block.configureCredentials("cje-slaves-freestyle-dmz": ["registry": "cred-id"])

    when: "executing a pod with containers"
    block.runPod({
      runSetup { withJenkins { echo "setup" } }

      failFast = false
      runContainer("registry/container1:latest") { withJenkins { echo "container1"} }
      runContainer("registry/container2:latest") { withJenkins { echo "container2"} }

      runTearDown { withJenkins { echo "teardown"}}
    })

    then: "the pod executes the containers"
    pipeline.anchor(SimFactory.echo("setup"))
      .nexts(SimFactory.echo("container1"), SimFactory.echo("container2"))
      .next(SimFactory.echo("teardown"))
  }

  def "pod should not be able to run without containers"() {
    given: "Container block and a closure for running in a pod"
    ContainerBlock block = new BlockContainerMock()
    block.ok = true

    and: "a pipeline configured with variable bindings for credential ids"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])
    pipeline.withEnv([JOB_NAME: this.getTestString(55)])
    block.jenkins = pipeline

    and: "docker secret is created"
    block.configureCredentials("cje-slaves-freestyle-dmz": ["registry": "cred-id"])

    when: "executing a pod without containers"
    block.runPod({
      runSetup { withJenkins { echo "setup" } }
      runTearDown { withJenkins { echo "teardown"}}
    })

    then: "the pod executes as expected"
    thrown(CirrusPipelineException)
  }

  def "container should only run inside a pod"() {
    given: "Container block and a closure for running in a pod"
    ContainerBlock block = new BlockContainerMock()
    block.ok = true

    and: "a pipeline configured with variable bindings for credential ids"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])
    pipeline.withEnv([JOB_NAME: this.getTestString(55)])
    block.jenkins = pipeline

    and: "docker secret is created"
    block.configureCredentials("cje-slaves-freestyle-dmz": ["registry": "cred-id"])

    when: "executing a container without a pod"
    block.runContainer("registry:container1:latest") { withJenkins { echo "container"} }

    then: "the execution fails"
    thrown(IllegalStateException)
  }

  def "containers cannot be nested only run inside a pod"() {
    given: "Container block and a closure for running in a pod"
    ContainerBlock block = new BlockContainerMock()
    block.ok = true

    and: "a pipeline configured with variable bindings for credential ids"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])
    pipeline.withEnv([JOB_NAME: this.getTestString(55)])
    block.jenkins = pipeline

    and: "docker secret is created"
    block.configureCredentials("cje-slaves-freestyle-dmz": ["registry": "cred-id"])

    when: "executing a pod with containers"
    block.runPod({
      runSetup { withJenkins { echo "setup" } }

      runContainer("registry:container1:latest") {
        runContainer("registry:container2:latest") {
          withJenkins { echo "container1"}
        }
      }
      runTearDown { withJenkins { echo "teardown"}}
    })

    then: "the execution fails"
    thrown(CirrusPipelineException)
  }

  static Result result = Result.ABORTED
  static CauseOfInterruption cause = new CauseOfInterruption.UserInterruption('mock-id')

  def "checkAborted should correctly identify aborted status"(Exception exception, boolean aborted) {
    given: "Container block"
    ContainerBlock block = new BlockContainerMock()

    expect:
    block.checkAborted(exception) == aborted

    where:
    exception                                                                                         | aborted
    null                                                                                              | false
    new CirrusPipelineException('abc')                                                                | false
    new CirrusPipelineException(new CirrusPipelineException('abc'))                                   | false
    new FlowInterruptedException(result, cause)                                                       | true
    new CirrusPipelineException(new FlowInterruptedException(result, cause))                          | true
    new CirrusPipelineException(new Exception(new FlowInterruptedException(result, cause)))           | true
  }

  def "setCurrentBuild should correctly set the aborted build status"() {
    given: "Container block and pipeline simulator"
    ContainerBlock block = new BlockContainerMock()
    JenkinsSim pipelineSim = createJenkinsSim()
    block.jenkins = pipelineSim

    when: "setCurrentBuild is called with FlowInterrurpted Exception"
    block.setCurrentBuildStatus(new CirrusPipelineException(new FlowInterruptedException(result, cause)))

    then: "the aborted build status is set"
    pipelineSim.currentBuild.result == 'ABORTED'
  }

  def "setCurrentBuild should correctly set the failure build status"() {
    given: "Container block and pipeline simulator"
    ContainerBlock block = new BlockContainerMock()
    JenkinsSim pipelineSim = createJenkinsSim()
    block.jenkins = pipelineSim

    when: "setCurrentBuild is called with an exception other than FlowInterrupted Exception"
    block.setCurrentBuildStatus(new CirrusPipelineException(new Exception()))

    then: "the failure build status is set"
    pipelineSim.currentBuild.result == 'FAILURE'
  }
}
