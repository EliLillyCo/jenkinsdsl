package com.lilly.cirrus.jenkinsdsl.sim

import com.lilly.cirrus.jenkinsdsl.core.CirrusPipelineException
import com.lilly.cirrus.jenkinsdsl.core.Scope

class JenkinsSim extends Scope implements PipelineMock {
  protected static int count = 0
  protected static int nextCount() {++count}

  Map<CommandSim, Closure> executionModel = [:]
  List<CommandSim> executionTrace = []
  Map<CommandSim, Throwable> exceptions = [:]
  Map<String, Map> credentialBindings = [:]
  def env = [:]
  def scm = [:]
  def currentBuild = [:]

  JenkinsSim openshift = this

  JenkinsSim withCredentialBinding(String credentialId, Map variableBindings) {
    this.credentialBindings[credentialId] = variableBindings
    return this
  }

  JenkinsSim withEnv(Map env) {
    this.env.putAll(env)
    return this
  }

  JenkinsSim withEnv(String key, String value) {
    this.env[key] = value
    return this
  }


  CommandSim command

  JenkinsSim when(CommandSim command) {
    this.command = command
    return this
  }

  JenkinsSim then(Closure<?> simulation) {
    if (!command) throw new IllegalStateException("then() must be called after calling when()")
    executionModel[command] = simulation
    command = null
    return this
  }

  def simulate(CommandSim command, def defaultReturnValue = null) {
    command.executionOrder = nextCount()
    executionTrace.add(command)

    Closure<?> simulationClosure = null
    executionModel.each { k, v ->
      if (command == k) {
        simulationClosure = v
        return
      }
    }

    if (simulationClosure) {
      try {
        return simulationClosure(command)
      }
      catch(Exception e) {
        this.exceptions[command] = e
        throw e
      }
    }

    return defaultReturnValue
  }

  CommandSim trace(CommandSim command) {
    for (int i = 0; i < executionTrace.size(); ++i) {
      CommandSim cmd = executionTrace.get(i)
      if (cmd == command) {
        anchor = cmd
        anchorIndex = i
        return anchor
      }
    }
    throw new IllegalStateException("No commands in the execution trace match the supplied command [${command}]")
  }

  CommandSim traceNext(CommandSim command) {
    checkAnchor()
    for (int i = anchorIndex + 1; i < executionTrace.size(); ++i) {
      CommandSim cmd = executionTrace.get(i)
      if (cmd == command) {
        anchor = cmd
        anchorIndex = i
        return cmd
      }
    }
    return null
  }

  CommandSim tracePrev(CommandSim command) {
    checkAnchor()
    for (int i = anchorIndex - 1; i >= 0; --i) {
      CommandSim cmd = executionTrace.get(i)
      if (cmd == command) {
        anchor = cmd
        anchorIndex = i
        return cmd
      }
    }
    return null
  }


  List<CommandSim> traceAll(CommandSim command) {
    List<CommandSim> list = []
    for (CommandSim cmd: executionTrace) {
      if (cmd == command) list.add(cmd)
    }
    return list
  }

  Map<CommandSim, Throwable> thrown(Class<? extends Throwable> exceptionType = null) {
    if (exceptionType) return exceptions.findAll {it.value.class == exceptionType}
    return exceptions
  }

  protected CommandSim anchor
  protected int anchorIndex

  JenkinsSim anchor(CommandSim command) {
    trace command
    return this
  }

  JenkinsSim anchorNth(CommandSim command, int index) {
    trace command
    for (int i = 0; i < index; ++i) {
      traceNext command
    }
    return this
  }

  void checkAnchor() {
    if (!anchor) throw new IllegalStateException("You must call anchor() before calling other trace checking methods")
  }

  JenkinsSim next(CommandSim command) {
    checkAnchor()
    CommandSim previousAnchor = anchor
    CommandSim nextCommand = traceNext command

    if (!nextCommand)
      throw new IllegalStateException("Command:\n[<${command.executionOrder}> ${command}] \nis not executed after: \n[<${previousAnchor.executionOrder}> ${previousAnchor}]\n")

    if (previousAnchor.executionOrder > nextCommand.executionOrder)
      throw new IllegalStateException("Command:\n [<${nextCommand.executionOrder}> ${nextCommand}] \nwas executed before:\n [<${previousAnchor.executionOrder}> ${previousAnchor}]")

    return  this
  }

  JenkinsSim nexts(CommandSim... commands) {
    checkAnchor()

    int highestOrder = 0
    int higestOrderIndex = -1

    CommandSim latestCommand
    CommandSim previousAnchor = anchor
    int previousAnchorIndex = anchorIndex

    for (int i = 0; i < commands.length; ++i) {
      CommandSim nextCommand = traceNext commands[i]

      if (!nextCommand)
        throw new IllegalStateException("Command:\n[${commands[i]}] \nis not executed after: \n[${previousAnchor}]\n")

      if (previousAnchor.executionOrder > nextCommand.executionOrder)
        throw new IllegalStateException("Command:\n[${nextCommand}] \nis executed before: \n[${previousAnchor}]\n")

      if (nextCommand.executionOrder > highestOrder) {
        highestOrder = nextCommand.executionOrder
        latestCommand = nextCommand
        higestOrderIndex = anchorIndex
      }

      anchor = previousAnchor
      anchorIndex = previousAnchorIndex
    }

    anchor = latestCommand
    anchorIndex = higestOrderIndex
    return  this
  }

  JenkinsSim prev(CommandSim command) {
    checkAnchor()
    CommandSim previousAnchor = anchor
    CommandSim previousCommand = tracePrev command

    if (!previousCommand)
      throw new IllegalStateException("Command:\n[${command}] \nis not executed before: \n[${previousAnchor}]\n")

    if (previousAnchor.executionOrder < previousCommand.executionOrder)
      throw new IllegalStateException("${previousCommand} was executed after ${previousAnchor}")

    return this
  }

  JenkinsSim prevs(CommandSim... commands) {
    checkAnchor()

    int lowestOrder = Integer.MAX_VALUE
    int lowestOrderIndex = -1

    CommandSim earliestCommand
    CommandSim previousAnchor = anchor
    int previousAnchorIndex = anchorIndex

    for (int i = 0; i < commands.length; ++i) {
      CommandSim prevCommand = tracePrev commands[i]

      if (!prevCommand)
        throw new IllegalStateException("No execution of ${commands[i]} found before ${previousAnchor}")

      if (previousAnchor.executionOrder < prevCommand.executionOrder)
        throw new IllegalStateException("${prevCommand} was executed after ${previousAnchor}")

      if (prevCommand.executionOrder < lowestOrder) {
        lowestOrder = prevCommand.executionOrder
        earliestCommand = prevCommand
        lowestOrderIndex = anchorIndex
      }
      anchor = previousAnchor
      anchorIndex = previousAnchorIndex
    }

    anchor = earliestCommand
    anchorIndex = lowestOrderIndex
    return  this
  }

  JenkinsSim rightShift(CommandSim command) {
    if (command instanceof CompositeCommandSim) return checkProfile(command.commands)
    else return anchor(command)
  }

  JenkinsSim checkProfile(List list) {
    anchor = null
    for (int i = 0; i < list.size(); ++i) {
      CommandSim cmd = list.get(i)
      cmd.executionOrder = i + 1

      if (!anchor) anchor(cmd)
      else next(cmd)
    }
    return this
  }

  @Override
  def sh(String command) {
    CommandSim cmd = SimFactory.sh(command)
    simulate(cmd, 0)
  }


  @Override
  def sh(Map args) {
    CommandSim cmd = SimFactory.sh(args)
    simulate(cmd, 0)
  }

  @Override
  def dir(String path, Closure<?> body) {
    CommandSim cmd = SimFactory.dir(path, body)
    simulate(cmd, 0)
  }

  @Override
  def echo(String text) {
    CommandSim cmd = SimFactory.echo(text)
    simulate(cmd, 0)
  }

  @Override
  def timeout(Map args, Closure<?> body) {
    CommandSim cmd = SimFactory.timeout(args, body)
    simulate(cmd, 0)
  }

  @Override
  def error(String text) {
    CommandSim cmd = SimFactory.error(text)
    simulate(cmd, 0)
    throw new CirrusPipelineException(text)
  }

  @Override
  def evaluate(String text) {
    CommandSim cmd = SimFactory.evaluate(text)
    simulate(cmd, 0)
  }

  @Override
  def stash(Map args) {
    CommandSim cmd = SimFactory.stash(args)
    simulate(cmd, 0)
  }


  @Override
  def unstash(String name) {
    CommandSim cmd = SimFactory.unstash(name)
    simulate(cmd, cmd)
  }

  @Override
  Map<String, String> checkout(def scm) {
    CommandSim cmd = SimFactory.checkout(scm)
    simulate(cmd, cmd.arguments)
  }

  @Override
  def stage(String name, Closure<?> body) {
    CommandSim cmd = SimFactory.stage(name, body)
    when cmd then body
    simulate(cmd, cmd.arguments)
  }


  @Override
  def parallel(Map... maps) {
    CommandSim cmd = SimFactory.parallel(maps)

    when cmd then {
      boolean failFast = cmd.arguments.failFast == null? true: cmd.arguments.failFast

      Exception exception = null
      cmd.arguments.each { key, closure ->
        if (closure instanceof Closure) {
          try {
            closure()
          }
          catch(Exception e) {
            if (failFast) throw e
            exception = e
          }
        }
      }
      if (exception) throw exception
    }

    simulate(cmd, 0)
  }

  @Override
  def containerTemplate(Map args) {
    CommandSim cmd = SimFactory.containerTemplate(args)
    simulate(cmd, cmd)
  }

  @Override
  def podTemplate(Map args, Closure<?> body) {
    CommandSim cmd = SimFactory.podTemplate(args, body)
    when cmd then { body() }
    simulate(cmd)
  }

  @Override
  def node(String label, Closure<?> body) {
    CommandSim cmd = SimFactory.node(label, body)
    when cmd then { body() }
    simulate(cmd, cmd)
  }

  @Override
  def container(String name, Closure<?> body) {
    CommandSim cmd = SimFactory.container(name, body)
    when cmd then { body() }
    simulate(cmd, cmd)
  }

  @Override
  def withCredentials(List credentials, Closure<?> body) {
    CommandSim cmd = SimFactory.withCredentials(credentials, body)
    when cmd then {
      body()
    }
    simulate(cmd, cmd)
  }

  @Override
  def usernamePassword(Map args) {
    CommandSim cmd = SimFactory.usernamePassword(args)
    simulateVariableBindings(cmd, args)
  }

  @Override
  def usernameColonPassword(Map args) {
    CommandSim cmd = SimFactory.usernameColonPassword(args)
    simulateVariableBindings(cmd, args)
  }

  @Override
  def certificate(Map args) {
    CommandSim cmd = SimFactory.certificate(args)
    simulateVariableBindings(cmd, args)
  }

  @Override
  def string(Map args) {
    CommandSim cmd = SimFactory.string(args)
    simulateVariableBindings(cmd, args)
  }

  @Override
  def file(Map args) {
    CommandSim cmd = SimFactory.file(args)
    simulateVariableBindings(cmd, args)
  }

  Map getCredentialBindings(String credentialsId) {
    Map bindings = credentialBindings[credentialsId]

    if (!bindings) {
      throw new CirrusPipelineException("Your test suite is missing binding for ${credentialsId}. " +
        "Please use PipelineSim.withCredentialsBinding() to setup this binding for your test suite.")
    }

    return bindings
  }

  def simulateVariableBindings(CommandSim cmd, Map args) {
    when cmd then {
      String credentialsId = args.credentialsId
      Map bindings = getCredentialBindings(credentialsId)

      args.each { key, value ->
        if (key != "credentialsId") this.setProperty(value, bindings[value])
      }
    }
    simulate(cmd, cmd)
  }

  @Override
  def selector(String... list) {
    CommandSim cmd = SimFactory.selector(list)
    simulate(cmd, this)
  }

  def describe() {
    CommandSim cmd = SimFactory.describe()
    simulate(cmd, cmd)
  }

  @Override
  def create(String... list) {
    CommandSim cmd = SimFactory.create(list)
    simulate(cmd, cmd)
  }

  @Override
  def create(List list) {
    CommandSim cmd = SimFactory.create(list)
    simulate(cmd, cmd)
  }

  @Override
  def secrets(String... list) {
    CommandSim cmd = SimFactory.secrets(list)
    simulate(cmd, cmd)
  }

  @Override
  def secrets(List list) {
    CommandSim cmd = SimFactory.secrets(list)
    simulate(cmd, cmd)
  }

  @Override
  def raw(String... list) {
    CommandSim cmd = SimFactory.raw(list)
    simulate(cmd, cmd)
  }

  @Override
  def raw(List list) {
    CommandSim cmd = SimFactory.raw(list)
    simulate(cmd, cmd)
  }

  @Override
  def delete(String... list) {
    CommandSim cmd = SimFactory.delete(list)
    simulate(cmd, cmd)
  }

  @Override
  def delete(List list) {
    CommandSim cmd = SimFactory.delete(list)
    simulate(cmd, cmd)
  }

  def delete() {
    CommandSim cmd = SimFactory.delete()
    simulate(cmd, cmd)
  }

  @Override
  def startBuild(String... list) {
    CommandSim cmd = SimFactory.startBuild(list)
    simulate(cmd, cmd)
  }

  @Override
  def startBuild(List list) {
    CommandSim cmd = SimFactory.startBuild(list)
    simulate(cmd, cmd)
  }

  def exists() {
    CommandSim cmd = SimFactory.exists()
    simulate(cmd, cmd)
  }

  @Override
  def withCluster(Closure body) {
    CommandSim cmd = SimFactory.withCluster(body)
    when cmd then { body() }
    simulate(cmd, cmd)
  }

  @Override
  def withCluster(String url, String token, Closure body) {
    CommandSim cmd = SimFactory.withCluster(url, token, body)
    when cmd then { body() }
    simulate(cmd, cmd)
  }

  @Override
  def withProject(String name, Closure body) {
    CommandSim cmd = SimFactory.withProject(name, body)
    when cmd then { body() }
    simulate(cmd, cmd)
  }

  @Override
  def withCredentials(String credentials, Closure<?> body) {
    CommandSim cmd = SimFactory.withCredentials(credentials, body)
    when cmd then {
      body()
    }
    simulate(cmd, cmd)
  }

  @Override
  def rollout() {
    CommandSim cmd = SimFactory.exists()
    simulate(cmd, this)
  }

  @Override
  def latest() {
    CommandSim cmd = SimFactory.exists()
    simulate(cmd, cmd)
  }

  @Override
  def readJSON(Map args) {
    CommandSim cmd = SimFactory.readJSON(args)
    simulate(cmd, cmd)
  }

  @Override
  def readYaml(Map args) {
    CommandSim cmd = SimFactory.readYaml(args)
    simulate(cmd, cmd)
  }

  @Override
  def readTrusted(String path) {
    CommandSim cmd = SimFactory.readTrusted(path)
    simulate(cmd, cmd)
  }

  @Override
  def readFile(Map args) {
    CommandSim cmd = SimFactory.readFile(args)
    simulate(cmd, cmd)
  }

  @Override
  def readFile(String path) {
    CommandSim cmd = SimFactory.readFile(path)
    simulate(cmd, cmd)
  }

  @Override
  def writeFile(Map args) {
    CommandSim cmd = SimFactory.writeFile(args)
    simulate(cmd, cmd)
  }

  @Override
  def fileExists(String path) {
    CommandSim cmd = SimFactory.fileExists(path)
    simulate(cmd, cmd)
  }

  @Override
  def touch(String path) {
    CommandSim cmd = SimFactory.touch(path)
    simulate(cmd, cmd)
  }

  @Override
  def load(String path) {
    CommandSim cmd = SimFactory.load(path)
    simulate(cmd, cmd)
  }

  @Override
  def zip(Map args) {
    CommandSim cmd = SimFactory.zip(args)
    simulate(cmd, cmd)
  }

  @Override
  def libraryResource(String path) {
    CommandSim cmd = SimFactory.libraryResource(path)
    simulate(cmd, cmd)
  }

  @Override
  def archiveArtifacts(String path) {
    archiveArtifacts([artifacts: path])
  }

  @Override
  def archiveArtifacts(Map args) {
    CommandSim cmd = SimFactory.archiveArtifacts(args)
    simulate(cmd, cmd)
  }


  @Override
  public String toString() {
    return "PipelineSim {...}"
  }
}
