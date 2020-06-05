package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.stage.StageScopeMock
import com.lilly.cirrus.jenkinsdsl.core.mock.stage.StageSubMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class StageSpec extends CirrusDSLSpec {
  Stage mockStage

  def setup() {
    mockStage = new Stage(body: {
      when {
        whenExecuted = true
        return whenExecuted
      }

      preScript {
        preScriptExecuted = true
      }

      dryScript {
        dryScriptExecuted = true
      }

      script {
        scriptExecuted = true
      }

      postScript {
        postScriptExecuted = true
      }
    })
  }

  def "getWhen() should return the when closure"() {
    when: "getWhen() is called"
    Closure<?> closure = mockStage.getWhen()

    then: "the right closure field must be returned"
    mockStage.when == closure
  }

  def "getPreScript() should return the preScript closure"() {
    when: "getPreScript() is called"
    Closure<?> closure = mockStage.getPreScript()

    then: "the right closure field must be returned"
    mockStage.preScript == closure
  }

  def "geDryScript() should return the dryScript closure"() {
    when: "geDryScript() is called"
    Closure<?> closure = mockStage.getDryScript()

    then: "the right closure field must be returned"
    mockStage.dryScript == closure
  }

  def "getScript() should return the script closure"() {
    when: "getScript() is called"
    Closure<?> closure = mockStage.getScript()

    then: "the right closure field must be returned"
    mockStage.script == closure
  }

  def "getPostScript() should return the postScript closure"() {
    when: "getPostScript() is called"
    Closure<?> closure = mockStage.getPostScript()

    then: "the right closure field must be returned"
    mockStage.postScript == closure

  }

  def "getBody() should return the body closure"() {
    when: "getBody() is called"
    Closure<?> closure = mockStage.getBody()

    then: "the right closure field must be returned"
    mockStage.body == closure
  }

  def "executing when with a closure should set the when field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "when() is called with the closure"
    mockStage.when closure

    then: "the when field must be set correctly"
    mockStage.when == closure
  }

  def "executing preScript with a closure should set the preScript field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "preScript() is called with the closure"
    mockStage.preScript closure

    then: "the preScript field must be set correctly"
    mockStage.preScript == closure
  }

  def "executing dryScript with a closure should set the dryScript field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "dryScript() is called with the closure"
    mockStage.dryScript closure

    then: "the dryScript field must be set correctly"
    mockStage.dryScript == closure
  }

  def "executing script with a closure should set the script field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "script() is called with the closure"
    mockStage.script closure

    then: "the script field must be set correctly"
    mockStage.script == closure
  }

  def "executing postScript with a closure should set the postScript field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "postScript() is called with the closure"
    mockStage.postScript closure

    then: "the postScript field must be set correctly"
    mockStage.postScript == closure
  }

  def "executing body with a closure should set the body field"() {
    given: "a closure"
    Closure<?> closure = {1 == 2}

    when: "body() is called with the closure"
    mockStage.body closure

    then: "the body field must be set correctly"
    mockStage.body == closure
  }

  def "a stage should not execute when the scope containing the stage is not ok to execute"() {
    given: "a scope with ok set to false and a mock stage"
    Scope scope = new Scope()
    scope.ok = false
    Stage stage = new Stage(body: {bodyExecuted = true})

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the body is not executed"
    !stage.bodyExecuted
  }

  def "a stage should execute when the scope containing the stage is ok to execute"() {
    given: "a scope with ok set to true and a mock stage with the jenkinsSim object"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    Stage stage = new Stage(script: {scriptExecuted = true})

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the script closure is executed"
    stage.scriptExecuted

    then: "the stage is executed on the jenkinsSim"
    jenkinsSim.trace SimFactory.stage(stage.name)
  }

  def "a stage should throw an Exception when the script closure is missing"() {
    given: "a scope with ok set to true and a mock stage"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    Stage stage = new Stage()

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "An exception is thrown due to missing script closure"
    thrown(CirrusPipelineException)
  }

  def "a stage postScript handles an exception when thrown in the preScript"() {
    given: "a scope with ok set to true"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    and: "a mock stage"
    Stage stage = new Stage(
      preScript: { throw new CirrusPipelineException("") },
      script: { scriptExecuted = true },
      postScript: { Exception e ->
          if (e) postScriptExecuted = true
      }
    )

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the preScript threw an exception"
    thrown(CirrusPipelineException)

    and: "the main script was NOT executed"
    !stage.scriptExecuted

    and: "the postScript was executed"
    stage.postScriptExecuted
  }

  def "a subclass of stage should be able to hook in the stage closures"() {
    given: "a scope with ok set to true and a subclass of stage with overridden closures"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    Stage stage =  new StageSubMock()

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the subclass when closure is executed"
    stage.whenExecuted

    then: "the subclass preScript closure is executed"
    stage.preScriptExecuted

    then: "the subclass body closure is executed"
    stage.bodyExecuted

    then: "the subclass dryScript is not executed as not dryRun flag is set"
    !stage.dryScriptExecuted

    then: "the subclass script is executed"
    stage.scriptExecuted

    then: "the subclass postScript is executed"
    stage.postScriptExecuted
  }


  def "a subclass of stage should be able to hook in the stage closures and run dry"() {
    given: "a scope with ok and dryRun set to true and a subclass of stage with overriden closures"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)
    scope.dryRun = true

    Stage stage =  new StageSubMock()

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the subclass when closure is executed"
    stage.whenExecuted

    then: "the subclass preScript is not executed as the dryRun flag is set to true"
    !stage.preScriptExecuted

    then: "the subclass body closure is executed"
    stage.bodyExecuted

    then: "the subclass dryScript closure is execute"
    stage.dryScriptExecuted

    then: "the subclass script is not executed as the dryRun flag is set to true"
    !stage.scriptExecuted

    then: "the subclass postScript is not executed as the dryRun flag is set to true"
    !stage.postScriptExecuted
  }

  def "when a Stage is executed, it should expand the scope of parent and should not run if it is not ok to do so"() {
    given: "a DSL block and its parent with ok set to false"
    Scope scope = new Scope()
    scope.pipeline = createJenkinsSim()
    scope.ok = false

    when: "execute is called on the block with a closure"
    mockStage.execute(scope)

    then: "it should expand the scope"
    mockStage.parent == scope

    and: "it should terminate without executing the closure"
    !mockStage.whenExecuted
  }

  def "when executing a stage, it should expand the scope of parent, run when ok to do so, and reduce the scope"() {
    given: "a scope with with ok set to true and a stage with pipeline configured"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    StageScopeMock stageMock = new StageScopeMock(script: {bodyExecuted = true})

    when: "execute is called on the block with a closure"
    stageMock.execute(scope)

    then: "it should expand the scope"
    stageMock.scopeExpanded

    then: "it should run the body"
    stageMock.bodyExecuted

    then: "it should reduce the scope"
    stageMock.scopeReduced
  }

  def "when executing a stage, it should not execute when it is not ok to do so"() {
    when: "exeuting a stage with ok set to false"
    mockStage.ok = false
    mockStage.execute(null)

    then: "it should not run the stage"
    !mockStage.whenExecuted
  }

  def "execution of script should invoke postScript action for a successful run"() {
    given: "a scope with ok set to true and a mock stage"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    Stage stage = new Stage(script: {scriptExecuted = true}, postScript: {postScriptExecuted = true})

    when: "the stage is executed with the scope"
    stage.execute(scope)

    then: "the script closure is executed on the pipeline object"
    stage.scriptExecuted

    then: "the postScript is executed on the stage"
    stage.postScriptExecuted
  }

  def "execution of script should invoke postScript action with an exception for an unsuccessful run"() {
    given: "a scope with ok set to true, a mock stage, and an exception"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)
    Exception exception = new CirrusPipelineException("runTest")

    Stage stage = new Stage(
      script: {
        scriptExecuted = true
        throw exception
      },
      postScript: {Exception e->
        caughtException = e
      })

    when: "the stage is executed with an erroneous script"
    stage.execute(scope)

    then: "the script closure is executed on the pipeline object"
    stage.scriptExecuted

    then: "the postScript is executed on the stage that receive the exception"
    stage.caughtException == exception

    then: "the exception is finally thrown to the Jenkins runtime"
    thrown(CirrusPipelineException)
  }

  def "execution of script should invoke default postScript action with an exception for an unsuccessful run"() {
    given: "a scope with ok set to true, a mock stage, and an exception"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    Exception exception = new CirrusPipelineException("runTest")

    Stage stage = new Stage(
      script: {
        scriptExecuted = true
        throw exception
      })

    when: "the stage is executed with an erroneous script"
    stage.execute(scope)

    then: "the script closure is executed on the pipeline object"
    stage.scriptExecuted

    then: "the postScript is executed on the stage and sets the stageconfig failed flag to true"
    stage.stageConfig.failed

    then: "the exception is finally thrown to the Jenkins runtime"
    thrown(CirrusPipelineException)
  }

  def "stage builder should build the stage correctly"() {
    given: "stage fields to be configured with the builder"
    Closure<?> when = {}
    Closure<?> preScript = {}
    Closure<?> dryScript = {}
    Closure<?> script = {}
    Closure<?> postScript = {}
    String name = "stage"

    when: "a stage is created with its builder"
    Stage stage = Stage.Builder.create()
      .withWhen(when)
      .withPreScript(preScript)
      .withDryScript(dryScript)
      .withScript(script)
      .withPostScript(postScript)
      .withName(name)
      .build()

    then: "the stage is created with the supplied fields"
    stage.when == when
    stage.preScript == preScript
    stage.dryScript == dryScript
    stage.script == script
    stage.postScript == postScript
    stage.name == name
  }

  def "stage builder should build the stage body correctly"() {
    given: "stage fields to be configured with the builder"
    Closure<?> when = {}
    Closure<?> preScript = {}
    Closure<?> dryScript = {}
    Closure<?> script = {}
    Closure<?> postScript = {}
    String name = "stage"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = createJenkinsSim()
    setupScope(scope, jenkinsSim)

    when: "a stage is created with its builder"
    Stage stage = Stage.Builder.create()
      .withName(name)
      .withBody({
        delegate.when(when)
        delegate.preScript(preScript)
        delegate.dryScript(dryScript)
        delegate.script(script)
        delegate.postScript(postScript)
      })
      .build()
    stage.execute(scope)

    then: "the stage is created with the supplied fields"
    stage.when == when
    stage.preScript == preScript
    stage.dryScript == dryScript
    stage.script == script
    stage.postScript == postScript
    stage.name == name
  }
}
