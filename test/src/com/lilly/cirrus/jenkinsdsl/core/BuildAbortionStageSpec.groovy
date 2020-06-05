package com.lilly.cirrus.jenkinsdsl.core


import com.lilly.cirrus.jenkinsdsl.sim.CirrusCoreSpec
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import static com.lilly.cirrus.jenkinsdsl.sim.SimFactory.*

class BuildAbortionStageSpec extends CirrusCoreSpec {
  def "the stage builder should correctly build the stage"() {
    given: "configurable fields for a shell stage"
    String name = "Abort Previous Build"
    Closure<?> when = {}
    Closure<?> script = {}
    Closure<?> postScript = {}

    when: "the builder builds the shell stage"
    BuildAbortionStage stage = BuildAbortionStage.Builder.create()
      .withName(name)
      .withWhen(when)
      .withScript(script)
      .withPostScript(postScript)
      .build()

    then: "the stage should be built correctly"
    stage.getName() == name
    stage.getWhen() == when
    stage.getPostScript() == postScript
    stage.getScript() == script
    stage.getDryScript() != null
  }

  def "should log start and end messages even when there are empty builds"() {
    given: "a jenkins build that has no previous build to abort"
    Pipeline aPipeline = new Pipeline()
    aPipeline.pod = 'pod'
    aPipeline.configured = false

    and: "a pipeline simulator configured with previous builds"
    JenkinsSim pipelineSim = createJenkinsSim()
    setupPreviousBuild(pipelineSim, 0, [:])

    and: "the DSL to run the abortion"
    Closure<?> dsl = {
      abortPreviousBuilds {
        root().configured = true
      }
    }

    when: "the pipeline is run with the supplied DSL"
    aPipeline.start(pipelineSim, dsl)

    then: "it should execute the configuration closure"
    aPipeline.configured

    and: "start and end messages are logged"
    pipelineSim >> echo("Checking for previous running builds to abort...") +
      echo("Check for previous running builds complete. Proceeding to next stage, if any.")
  }

  def "should log start and end messages when there are no builds"() {
    given: "a jenkins build that has no previous build to abort"
    Pipeline aPipeline = new Pipeline()
    aPipeline.pod = 'pod'
    aPipeline.configured = false

    and: "a pipeline simulator configured with previous builds"
    JenkinsSim pipelineSim = createJenkinsSim()
    setupPreviousBuild(pipelineSim, 0, null)

    and: "the DSL to run the abortion"
    Closure<?> dsl = {
      abortPreviousBuilds {
        root().configured = true
      }
    }

    when: "the pipeline is run with the supplied DSL"
    aPipeline.start(pipelineSim, dsl)

    then: "it should execute the configuration closure"
    aPipeline.configured

    and: "start and end messages are logged"
    pipelineSim >> echo("Checking for previous running builds to abort...") +
      echo("Check for previous running builds complete. Proceeding to next stage, if any.")
  }

  def "should log start and end messages when there are previous builds that are not running"() {
    given: "a jenkins build that has no previous build to abort"
    Pipeline aPipeline = new Pipeline()
    aPipeline.pod = 'pod'
    aPipeline.configured = false

    and: "a pipeline simulator configured with previous builds"
    JenkinsSim pipelineSim = createJenkinsSim()
    def builds = setupPreviousBuild(pipelineSim, 0, [1: false, 2: false])

    and: "the DSL to run the abortion"
    Closure<?> dsl = {
      abortPreviousBuilds {
        root().configured = true
      }
    }

    when: "the pipeline is run with the supplied DSL"
    aPipeline.start(pipelineSim, dsl)

    then: "it should execute the configuration closure"
    aPipeline.configured

    and: "start and end messages are logged"
    pipelineSim >> echo("Checking for previous running builds to abort...") +
      echo("Check for previous running builds complete. Proceeding to next stage, if any.")

    and: 'dokill() was not called on the build objects'
    0 * builds[0].doKill()
    0 * builds[1].doKill()
  }

  def "should log start and end messages and kill the previous builds that are running while skipping the current build"() {
    given: "a jenkins build that has a previous build to abort"
    Pipeline aPipeline = new Pipeline()
    aPipeline.pod = 'pod'
    aPipeline.configured = false

    and: "a pipeline simulator configured with previous builds"
    JenkinsSim pipelineSim = createJenkinsSim()
    def builds = setupPreviousBuild(pipelineSim, 0, [0: true, 1: false, 2: true, 3: true, 4: false])

    and: "the DSL to run the abortion"
    Closure<?> dsl = {
      abortPreviousBuilds {
        root().configured = true
      }
    }

    when: "the pipeline is run with the supplied DSL"
    aPipeline.start(pipelineSim, dsl)

    then: "it should execute the configuration closure"
    aPipeline.configured

    and: "start and end messages are logged along with build abortion messages in between"
    pipelineSim >> echo("Checking for previous running builds to abort...") +
      echo(">> Aborting build #2") +
      echo(">> Aborting build #3") +
      echo("Check for previous running builds complete. Proceeding to next stage, if any.")

    and: "the doKill operation is not called on the current build but only on the active previous builds"
    0 * builds[0].doKill()
    0 * builds[1].doKill()
    1 * builds[2].doKill()
    1 * builds[3].doKill()
    0 * builds[4].doKill()
  }
}
