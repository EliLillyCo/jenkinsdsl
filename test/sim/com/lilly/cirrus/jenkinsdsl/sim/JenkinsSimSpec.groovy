package com.lilly.cirrus.jenkinsdsl.sim

class JenkinsSimSpec extends CirrusDSLSpec {
  def "nextCount should return the next count"() {
    given: "a starting count"
    int count = JenkinsSim.count

    when: "next count is called"
    int next = JenkinsSim.nextCount()

    then: "next count should increment by 1"
    next == count + 1
  }

  def "pipeline shold be able to be configured with an env"() {
    when: "pipeline is configured with an env"
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withEnv([BRANCH_NAME: "test"])

    then: "it should be able to use the env"
    pipelineSim.env.BRANCH_NAME == "test"
  }

  def "pipeline shold be able to be supplied with credential bindings"() {
    when: "pipeline is configured with an env"
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withCredentialBinding("myCredId", [USER: "test"])

    then: "it should be able to use the env"
    pipelineSim.credentialBindings["myCredId"].USER == "test"
  }

  def "the simulator should be able to specify the simulation logic for a command"() {
    given: "a pipeline simulator, a command, and a simulation logic"
    JenkinsSim pipeline = new JenkinsSim()
    CommandSim command = SimFactory.echo("test")
    Closure<?> simulation = {1 == 1}

    when: "a simulation logic set up"
    pipeline.when command then simulation

    then: "the executionModel should record the simulation"
    pipeline.executionModel[command] == simulation

    and: "the temporary command field is reset"
    !pipeline.command
  }

  def "the simulator should not accept simulation without a command"() {
    given: "a pipeline simulator, a command, and a simulation logic"
    JenkinsSim pipeline = new JenkinsSim()
    Closure<?> simulation = {1 == 1}

    when: "a simulation logic set up"
    pipeline.then simulation

    then: "an exception is thrown"
    thrown(IllegalStateException)
  }

  int returnValue
  def "simulating a command should execute the simulation logic associated with it"() {
    given: "a pipeline simulator, a command, and a configured simulation logic"
    JenkinsSim pipeline = new JenkinsSim()
    CommandSim command = SimFactory.sh("ls -lah")
    int previousExecutionOrder = command.executionOrder
    returnValue = 0

    Closure<?> simulation = {
      returnValue = 1
      return returnValue
    }

    pipeline.when command then simulation

    when: "the command is simulated"
    int actualReturnValue = pipeline.simulate command, 0

    then: "the command is given a new execution order"
    command.executionOrder > previousExecutionOrder

    then: "the command is added to the executionTrace"
    pipeline.executionTrace.contains(command)

    then: "the simulation is run"
    returnValue == 1

    then: "the value returned by the simulation closure is returned"
    actualReturnValue == returnValue
  }


  def "simulation tracks exceptions thrown during the execution"() {
    given: "a pipeline simulator, a command, and a configured simulation logic to simulate error"
    JenkinsSim pipeline = new JenkinsSim()
    CommandSim command = SimFactory.sh("ls -lah")

    Closure<?> simulation = {
      throw new Exception("Test")
    }

    pipeline.when command then simulation

    when: "the command is simulated"
    pipeline.simulate command, 0

    then: "the command is added to the executionTrace"
    pipeline.executionTrace.contains(command)

    then: "the simulation records the exception"
    pipeline.exceptions

    then: "the exception is thrown to the client code"
    thrown(Exception)
  }

  def "simulating a command without the associated simulation logic should return the default value"() {
    given: "a pipeline simulator and a command"
    JenkinsSim pipeline = new JenkinsSim()
    CommandSim command = SimFactory.sh("ls -lah")
    int defaultReturnValue = 10

    when: "the command is simulated"
    int returnValue = pipeline.simulate command, defaultReturnValue

    then: "the command is added to the executionTrace"
    pipeline.executionTrace.contains(command)

    then: "the simulation returns the default value"
    returnValue == defaultReturnValue
  }

  def "sh simulation should return default status of 0"() {
    given: "a pipeline simulator"
    JenkinsSim pipeline = new JenkinsSim()

    when: "a simple sh command is run"
    int returnValue = pipeline.sh("ls -lah")

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.sh("ls -lah")

    and: "the simulation returns the default status of 0"
    returnValue == 0
  }

  def "sh default behavior should be able to be overriden"() {
    given: "a pipeline simulator and new behavior configuration"
    JenkinsSim pipeline = new JenkinsSim()
    String lsLah = "ls -lah"

    pipeline.when SimFactory.sh(lsLah) then { return 1 }

    when: "a simple sh command is run"
    int returnValue = pipeline.sh(lsLah)

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.sh(lsLah)

    and: "the simulation returns the overriden status of 1"
    returnValue == 1
  }

  def "parameterized sh simulation should return the default status"() {
    given: "a pipeline simulator"
    JenkinsSim pipeline = new JenkinsSim()
    String lsLah = "ls -lah"

    when: "a parameterized sh command is run"
    int returnValue = pipeline.sh script: lsLah, returnStdout: true

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.sh(script: lsLah, returnStdout: true)

    and: "the simulation returns the default status of 0"
    returnValue == 0
  }

  def "echo simulation should return default status of 0"() {
    given: "a pipeline simulator"
    JenkinsSim pipeline = new JenkinsSim()

    when: "a simple echo command is run"
    int returnValue = pipeline.echo "hello"

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.echo("hello")

    and: "the simulation returns the default status of 0"
    returnValue == 0
  }

  def "stash simulation should return default status of 0"() {
    given: "a pipeline simulator"
    JenkinsSim pipeline = new JenkinsSim()

    when: "an stash command is run"
    int returnValue = pipeline.stash name: "stash", includes: "abc.txt"

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.stash(name: "stash", includes: "abc.txt")

    and: "the simulation returns the default status of 0"
    returnValue == 0
  }

  def "checkout simulation should return the checkout vars"() {
    given: "a pipeline simulator"
    JenkinsSim pipeline = new JenkinsSim()

    when: "an checkout command is run"
    def returnValue = pipeline.checkout pipeline.scm

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.checkout(pipeline.scm)

    and: "the simulation returns the default status of 0"
    returnValue == [scm: pipeline.scm]
  }

  def "stage simulation should execute the stage closure"() {
    given: "a pipeline simulator and a stage properties"
    JenkinsSim pipeline = new JenkinsSim()
    String stageName = "Build"
    Closure<?> stageBody = { return "npm run build" }


    when: "a stage command is run"
    def returnValue = pipeline.stage(stageName, stageBody)

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.stage(stageName, stageBody)

    and: "the simulation returns the expected value"
    returnValue == "npm run build"
  }

  def parallelExecutions = [:]
  def "parallel simulation should execute the parallel body"() {
    given: "a pipeline simulator and properties for parallel tasks"
    JenkinsSim pipeline = new JenkinsSim()
    parallelExecutions.clear()
    def parallelTasks = [
      "first": {parallelExecutions.first = true},
      "second": {parallelExecutions.second = true}
    ]

    when: "a parallel command is run"
    pipeline.parallel(parallelTasks)

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.parallel(parallelTasks)

    and: "all the parallel tasks are executed"
    parallelExecutions.first
    parallelExecutions.second
  }

  def "parallel simulation by default fails fast"() {
    given: "a pipeline simulator and properties for parallel tasks"
    JenkinsSim pipeline = new JenkinsSim()
    parallelExecutions.clear()
    def parallelTasks = [
      "first": {parallelExecutions.first = true},
      "second": {throw new Exception("test")},
      "third": {parallelExecutions.third = true}
    ]

    when: "a parallel command is run"
    pipeline.parallel(parallelTasks)

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.parallel(parallelTasks)

    and: "only the first task was run"
    parallelExecutions.first
    !parallelExecutions.third

    and: "the exception from second task is thrown"
    thrown(Exception)
  }

  def "parallel simulation should execute all tasks before failing when fastFast is disabled"() {
    given: "a pipeline simulator and properties for parallel tasks"
    JenkinsSim pipeline = new JenkinsSim()
    parallelExecutions.clear()
    def parallelTasks = [
      failFast: false,
      first: {parallelExecutions.first = true},
      second: {
        parallelExecutions.second = true
        throw new Exception("test")
      },
      third: {parallelExecutions.third = true}
    ]

    when: "a parallel command is run"
    pipeline.parallel(parallelTasks)

    then: "the command can be traced in the pipeline"
    pipeline.trace SimFactory.parallel(parallelTasks)

    and: "all the tasks are run"
    parallelExecutions.first
    parallelExecutions.second
    parallelExecutions.third

    and: "the exception from second task is thrown"
    thrown(Exception)
  }

  JenkinsSim pipelineExpSim
  def "pipeline records the execution of all tasks"() {
    given: "a pipeline and parallel tasks that invoke the pipeline"
    pipelineExpSim = new JenkinsSim()
    def parallelTasks = [
      failFast: false,
      first: {
        pipelineExpSim.echo("first")
      },
      second: {
        pipelineExpSim.sh("second")
      }
    ]

    when: "the parallel command is followed by an echo command on the pipeline"
    pipelineExpSim.echo("start")
    pipelineExpSim.parallel(parallelTasks)
    pipelineExpSim.echo("end")

    then: "all the commands can be traced in the pipeline"
    pipelineExpSim.trace SimFactory.echo("start")
    pipelineExpSim.trace SimFactory.parallel(parallelTasks)
    pipelineExpSim.trace SimFactory.echo("first")
    pipelineExpSim.trace SimFactory.sh("second")
    pipelineExpSim.trace SimFactory.echo("end")

    and: "parallel tasks are run parallely after the start"
    pipelineExpSim.anchor(SimFactory.echo("start"))
      .next(SimFactory.parallel(parallelTasks))
      .nexts(SimFactory.echo("first"), SimFactory.sh("second"))
      .next(SimFactory.echo("end"))

    and: "parallel tasks are run parallely after the start irrespective of execution order"
    pipelineExpSim.anchor(SimFactory.echo("start"))
      .next(SimFactory.parallel(parallelTasks))
      .nexts(SimFactory.sh("second"), SimFactory.echo("first"))
      .next(SimFactory.echo("end"))

    and: "parallel tasks are run parallely before the end command"
    pipelineExpSim.anchor(SimFactory.echo("end"))
      .prevs(SimFactory.echo("first"), SimFactory.sh("second"))
      .prev(SimFactory.parallel(parallelTasks))
      .prev(SimFactory.echo("start"))

    and: "parallel tasks are run parallely before the end command irrespective of execution order"
    pipelineExpSim.anchor(SimFactory.echo("end"))
      .prevs(SimFactory.sh("second"), SimFactory.echo("first"))
      .prev(SimFactory.parallel(parallelTasks))
      .prev(SimFactory.echo("start"))

    and: "parallel tasks are run parallel to each other"
    pipelineExpSim.anchor(SimFactory.echo("start"))
      .next(SimFactory.parallel(parallelTasks))
      .next(SimFactory.echo("first"))
      .anchor(SimFactory.parallel(parallelTasks))
      .next(SimFactory.sh("second"))
      .next(SimFactory.echo("end"))
  }

  def "pipeline traces the error while executing tasks"() {
    given: "a pipeline and parallel tasks that invoke the pipeline"
    pipelineExpSim = new JenkinsSim()
    def parallelTasks = [
      failFast: false,
      first: {
        pipelineExpSim.sh("first")
      },
      second: {
        pipelineExpSim.sh("second")
      }
    ]

    pipelineExpSim.when SimFactory.sh("first") then {throw new Exception("first")}
    pipelineExpSim.when SimFactory.sh("second") then {throw new Exception("second")}

    when: "the parallel command is followed by an echo command on the pipeline"
    pipelineExpSim.echo("start")
    pipelineExpSim.parallel(parallelTasks)

    then: "all the commands can be traced in the pipeline"
    pipelineExpSim.trace SimFactory.echo("start")
    pipelineExpSim.trace SimFactory.parallel(parallelTasks)
    pipelineExpSim.trace SimFactory.sh("first")
    pipelineExpSim.trace SimFactory.sh("second")

    and: "parallel tasks are run parallely after the start"
    pipelineExpSim.anchor(SimFactory.echo("start"))
      .next(SimFactory.parallel(parallelTasks))
      .nexts(SimFactory.sh("first"), SimFactory.sh("second"))

    and: "exception was thrown to the client"
    thrown(Exception)

    and: "the pipeline tracks all exceptions"
    pipelineExpSim.thrown()
    pipelineExpSim.thrown(Exception)
    !pipelineExpSim.thrown(NullPointerException)


    and: "the pipeline knows the command that threw the given exception"
    pipelineExpSim.thrown(Exception)[SimFactory.sh("first")]
    pipelineExpSim.thrown(Exception)[SimFactory.sh("second")]
    !pipelineExpSim.thrown(Exception)[SimFactory.sh("start")]
  }

  def "pipeline can trace similar commands"() {
    given:
    pipelineExpSim = new JenkinsSim()

    when: "executing multiple similar commands"
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")
    pipelineExpSim.sh("ls")
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    then: "pipeline can track all similar commands"
    pipelineExpSim.traceAll(SimFactory.sh("ls")).size() == 3
    pipelineExpSim.traceAll(SimFactory.echo("hello")).size() == 2

    and: "pipeline know the commands that were not executed"
    !pipelineExpSim.traceAll(SimFactory.echo("hi"))
    !pipelineExpSim.anchor(SimFactory.sh("ls")).tracePrev(SimFactory.echo("hi"))
    !pipelineExpSim.anchor(SimFactory.sh("ls")).traceNext(SimFactory.echo("hi"))
  }

  def "pipeline knows the commands that were not executed"() {
    given:
    pipelineExpSim = new JenkinsSim()

    when: "executing multiple similar commands"
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")
    pipelineExpSim.sh("ls")

    then: "pipeline know the commands that were not executed"
    !pipelineExpSim.traceAll(SimFactory.echo("hi"))
    !pipelineExpSim.anchor(SimFactory.sh("ls")).tracePrev(SimFactory.echo("hi"))
    !pipelineExpSim.anchor(SimFactory.sh("ls")).traceNext(SimFactory.echo("hi"))
  }

  def "pipeline can anchor to nth similar commands that were executed"() {
    given:
    pipelineExpSim = new JenkinsSim()

    when: "executing multiple similar commands"
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hi")
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("bye")

    then: "pipeline know the commands that were executed"
    pipelineExpSim.anchorNth(SimFactory.sh("ls"), 0).next(SimFactory.echo("hello"))
    pipelineExpSim.anchorNth(SimFactory.sh("ls"), 1).next(SimFactory.echo("hi"))
    pipelineExpSim.anchorNth(SimFactory.sh("ls"), 2).next(SimFactory.echo("bye"))
  }


  def "pipeline knows the if a command did not execute next"() {
    given: "A pipeline execution"
    pipelineExpSim = new JenkinsSim()
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    when: "pipeline is queried about the next executed command"
    pipelineExpSim.anchor(SimFactory.echo("hello"))
      .next(SimFactory.sh("ls"))

    then: "pipeline knows the the order mismatch"
    thrown(IllegalStateException)
  }

  def "pipeline knows the if commands did not execute next"() {
    given: "A pipeline execution"
    pipelineExpSim = new JenkinsSim()
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    when: "pipeline is quiried about the next executed command"
    pipelineExpSim.anchor(SimFactory.echo("hello"))
      .nexts(SimFactory.sh("ls"), SimFactory.echo("hello"))

    then: "pipeline knows the the order mismatch"
    thrown(IllegalStateException)
  }

  def "pipeline knows the if a command did not execute previously"() {
    given: "A pipeline execution"
    pipelineExpSim = new JenkinsSim()
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    when: "pipeline is quiried about the next executed command"
    pipelineExpSim.anchor(SimFactory.echo("hello"))
      .prev(SimFactory.sh("hi"))

    then: "pipeline knows the the order mismatch"
    thrown(IllegalStateException)
  }

  def "pipeline knows the if an executed command did not execute before a given command"() {
    given: "A pipeline execution"
    pipelineExpSim = new JenkinsSim()
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    when: "pipeline is quiried about the next executed command"
    pipelineExpSim.anchor(SimFactory.echo("ls"))
      .prev(SimFactory.sh("hello"))

    then: "pipeline knows the the order mismatch"
    thrown(IllegalStateException)
  }

  def "pipeline knows the if commands did not execute previously"() {
    given: "A pipeline execution"
    pipelineExpSim = new JenkinsSim()
    pipelineExpSim.sh("ls")
    pipelineExpSim.echo("hello")

    when: "pipeline is queried about the next executed command"
    pipelineExpSim.anchor(SimFactory.echo("hello"))
      .prevs(SimFactory.sh("ls"), SimFactory.echo("hi"))

    then: "pipeline knows the the order mismatch"
    thrown(IllegalStateException)
  }

  def "pipeline should be able to use usernameColorPassword binding"() {
    given:
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withCredentialBinding("credId", [USERPASS: "user:password"])

    when: "usernameColonPassword credential is used"
    pipelineSim.usernameColonPassword([credentialsId: "credId", variable: "USERPASS"])

    then: "the new variable is bound to the value in the pipeline"
    pipelineSim.USERPASS == "user:password"
  }

  def "pipeline should be able to use string binding"() {
    given:
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withCredentialBinding("credId", [DATA: "some data"])

    when: "string credential is used"
    pipelineSim.string([credentialsId: "credId", variable: "DATA"])

    then: "the new variable is bound to the value in the pipeline"
    pipelineSim.DATA == "some data"
  }

  def "pipeline should be able to use file binding"() {
    given:
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withCredentialBinding("credId", [FILE: "a file"])

    when: "file credential is used"
    pipelineSim.file([credentialsId: "credId", variable: "FILE"])

    then: "the new variable is bound to the value in the pipeline"
    pipelineSim.FILE == "a file"
  }

  def "pipeline should throw an exception if a binding is missing"() {
    given:
    JenkinsSim pipelineSim = new JenkinsSim()
    pipelineSim.withCredentialBinding("randomId", [FILE: "a file"])

    when: "file credential is used"
    pipelineSim.file([credentialsId: "credId", variable: "FILE"])

    then: "an exception is thrown"
    thrown(Exception)
  }

  def "pipeline should throw an exception if binding is used without providing mock binding"() {
    given:
    JenkinsSim pipelineSim = new JenkinsSim()

    when: "file credential is used"
    pipelineSim.file([credentialsId: "credId", variable: "FILE"])

    then: "an exception is thrown"
    thrown(Exception)
  }
}
