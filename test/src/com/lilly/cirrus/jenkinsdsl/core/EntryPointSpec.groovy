package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.entrypoint.EntryPointMock

class EntryPointSpec extends CirrusDSLSpec {


  def "start() should start the pipeline by correctly setting pipeline variables"() {
    given: "an entry point mock, a pipeline object with mock environment, and a closure"
    Scope pipeline = new Scope()
    pipeline.env = [BRANCH_NAME: "master"]
    EntryPointMock entry = new EntryPointMock()
    Closure<?> body = {bodyExecuted = true}

    when: "starting a mock entry point with the pipeline object"
    entry.start(pipeline, body)

    then: "pipeline field must be properly set"
    entry.jenkins == pipeline

    then: "environment field must be propertly set"
    entry.env == pipeline.env

    then: "pipeline should be ok to run"
    entry.ok

    then: "the pipeline body should be executed on the entry object"
    entry.bodyExecuted
  }

  def "start() should throw execution exception to Jenkins runtime"() {
    given: "an entry point mock, a pipeline object with mock environment, and an erroneous closure"
    Scope pipeline = new Scope()
    pipeline.env = [BRANCH_NAME: "master"]
    EntryPointMock entry = new EntryPointMock()
    Closure<?> body = {throw new Exception("Dummy Error")}

    when: "starting a mock entry point with the pipeline object"
    entry.start(pipeline, body)

    then: "pipeline field must be properly set"
    entry.jenkins == pipeline

    then: "environment field must be propertly set"
    entry.env == pipeline.env

    then: "pipeline should be ok to run"
    entry.ok

    then: "the pipeline body should be executed on the entry object"
    thrown(CirrusPipelineException)
  }
}
