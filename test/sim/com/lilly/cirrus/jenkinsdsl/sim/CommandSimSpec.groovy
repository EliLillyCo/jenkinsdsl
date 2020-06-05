package com.lilly.cirrus.jenkinsdsl.sim

class CommandSimSpec extends CirrusDSLSpec {
  CommandSim commandSim

  String type = "sh"
  def arguments = [text: "ls -lah"]
  int order = 1
  Closure<?> body = {1 == 1}

  def setup() {
    commandSim = new CommandSim()
    commandSim.setType(type)
    commandSim.setArguments(arguments)
    commandSim.setExecutionOrder(order)
    commandSim.setBody(body)
  }

  def "Can use getters and setters to access and mutate a command"() {
    expect: "that getters can be used to access a commands' fields"
    commandSim.getType() == type
    commandSim.getArguments() == arguments
    commandSim.getExecutionOrder() == order
    commandSim.getBody() == body
  }

  def "a command should be equal to itself"() {
    expect:
    commandSim == commandSim
    new CommandSim() == new CommandSim()
  }

  def "equal command should have equal hashCode"() {
    expect:
    commandSim.hashCode() == commandSim.hashCode()
    new CommandSim().hashCode() == new CommandSim().hashCode()
    new CommandSim(arguments: null).hashCode() == new CommandSim(arguments: null).hashCode()
    new CommandSim(type: null).hashCode() == new CommandSim(type: null).hashCode()
    new CommandSim(type: null, arguments: null).hashCode() == new CommandSim(type: null, arguments: null).hashCode()
    new CommandSim(type: "sh", arguments: null).hashCode() == new CommandSim(type: "sh", arguments: null).hashCode()
    new CommandSim(type: null, arguments: [:]).hashCode() == new CommandSim(type: null, arguments: [:]).hashCode()
  }


  def "equal command with arguments command should have equal hashCode"() {
    given:
    String curl = "curl -H \"X-JFrog-Art-Api:api-token\" -X GET https://elilillyco.jfrog.io/elilillyco/api/storage/cirr-dynamic-docker-lc/node".trim()

    String dockerPassword = "api-token"
    String artifactoryServer = "https://elilillyco.jfrog.io/elilillyco"
    String artifactoryImageRepo = "cirr-dynamic-docker-lc"
    String name = "node"

    String anotherCurl = "curl -H \"X-JFrog-Art-Api:${dockerPassword}\" -X GET " +
      "${artifactoryServer}/api/storage/${artifactoryImageRepo}/${name}"

    CommandSim commandSim1 = SimFactory.sh(script: curl, returnStdout: true)
    CommandSim commandSim2 = SimFactory.sh(script: anotherCurl, returnStdout: true)

    expect:
    commandSim1 == commandSim2
    commandSim1.hashCode() == commandSim2.hashCode()
  }


  def "a command should not be equal to different types and commands"() {
    expect:
    new CommandSim(type: null) != new CommandSim(type: "bcd")
    new CommandSim(type: "sh") != new CommandSim(type: "bcd")
    new CommandSim(type: "sh", arguments: null) != new CommandSim(type: "sh", arguments: [:])
    new CommandSim(type: "sh", arguments: null) != new CommandSim(type: "sh", arguments: [a: "a"])
  }

  def "a command should generate its string as expected"() {
    expect:
    commandSim.toString() == "${type} text: ${arguments.text}"
  }

  def "an empty command should still have a string representation"() {
    expect:
    new CommandSim().toString() == "CommandSim"
    new CommandSim(arguments: null).toString() == "CommandSim"
  }

  def "a command should properly include arguments in its string representation"() {
    expect:
    new CommandSim(arguments: [a: "apple", b: "ball"]).toString() == "CommandSim a: apple, b: ball"
  }

  def "commands should match the wildcard"() {
    expect:
    new CommandSim(type: null) == _
    new CommandSim(type: "sh") == _
    new CommandSim(type: "sh", arguments: null) == _
    new CommandSim(type: "sh", arguments: [:]) == _
    new CommandSim(type: "sh", arguments: [a: "a"]) == _
  }

  def "commands should match commands with correct wildcard use"() {
    expect:
    new CommandSim() != null
    new CommandSim(type: null) == new CommandSim(type: _)
    new CommandSim(type: "sh") == new CommandSim(type: _)
    new CommandSim(type: "sh", arguments: null) == new CommandSim(type: "sh", arguments: _)
    new CommandSim(type: "sh", arguments: [:]) == new CommandSim(type: _, arguments: _)
    new CommandSim(type: "sh", arguments: [a: "a"]) == new CommandSim(type: _, arguments: _)
    new CommandSim(type: "sh", arguments: [a: "a", b: "b"]) == new CommandSim(type: _, arguments: [a: _, b: "b"])
    new CommandSim(type: "sh", arguments: [a: "a", b: "b"]) == new CommandSim(type: _, arguments: [a: _, b: _])

    new CommandSim(type: "sh") != new CommandSim(type: "echo")
    new CommandSim(type: "sh", arguments: [a: "a"]) != new CommandSim(type: _, arguments: [:])
    new CommandSim(type: "sh", arguments: [a: "a"]) != new CommandSim(type: null, arguments: _)
    new CommandSim(type: "sh", arguments: [a: "a", b: "b"]) != new CommandSim(type: _, arguments: [a: _])
    new CommandSim(type: "sh", arguments: [a: "a", b: "b"]) != new CommandSim(type: _, arguments: [a: _, b: "c"])
  }

  def "a command should match any command based on supplied closures"() {
    given:
    CommandSim any = SimFactory.any()

    expect:
    any == any
    new CommandSim(type: "sh", arguments: null) == any
    new CommandSim(type: "sh", arguments: [:]) == any
    new CommandSim(type: "sh", arguments: [a: "a"]) == any

    new CommandSim(type: null) == new CommandSim(type: { type -> !type})
    new CommandSim(type: "sh") == new CommandSim(type: { type -> type == "sh"})

    new CommandSim(type: "sh", arguments: [a: "a"]) == new CommandSim(type: _, arguments: { map -> map.a == "a"})

    new CommandSim(type: "sh", arguments: [a: "a"]) ==
      new CommandSim(type: { type -> type == 'sh'}, arguments: { map -> map.a == "a"})

    new CommandSim(type: "sh", arguments: [script: "ls -lah ", returnStdOut: true]) ==
      new CommandSim(type: { type -> type == 'sh'}, arguments: { map -> map.script && map.returnStdOut})


    any != "random"
    new CommandSim(type: "sh", arguments: [script: "ls -lah ", returnStdOut: true]) !=  new CommandSim(type: { false })
    new CommandSim(type: "sh", arguments: [script: "ls -lah ", returnStdOut: true]) !=
      new CommandSim(type: _, arguments: { false })
  }

  def "call to minus creates a composite command with correct ordering"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")

    when:
    CompositeCommandSim composite = echo2 - echo1

    then:
    composite.commands == [echo1, echo2]
  }

  def "call to plus creates a composite command with correct ordering"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")

    when:
    CompositeCommandSim composite = echo1 + echo2

    then:
    composite.commands == [echo1, echo2]
  }

}
