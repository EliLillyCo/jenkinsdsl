package com.lilly.cirrus.jenkinsdsl.sim

class CompositeCommandSpec extends CirrusDSLSpec {
  def "Composite command plus works as expected"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")
    CommandSim echo3 = SimFactory.echo("third")

    when:
    CompositeCommandSim composite = echo1 + echo2 + echo3

    then:
    composite.commands == [echo1, echo2, echo3]
  }

  def "Composite command minus works as expected"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")
    CommandSim echo3 = SimFactory.echo("third")

    when:
    CompositeCommandSim composite = echo3 - echo2 - echo1

    then:
    composite.commands == [echo1, echo2, echo3]
  }

  def "Composite command plus and minus combination works as expected"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")
    CommandSim echo3 = SimFactory.echo("third")

    when:
    CompositeCommandSim composite1 = echo2 - echo1
    CompositeCommandSim composite2 = echo2 + echo3
    CompositeCommandSim composite3 = echo1 + composite1 + composite2 + echo3

    then:
    composite3.commands == [echo1, echo1, echo2, echo2, echo3, echo3]
  }

  def "Composite command minus and plus combination works as expected"() {
    given:
    CommandSim echo1 = SimFactory.echo("first")
    CommandSim echo2 = SimFactory.echo("second")
    CommandSim echo3 = SimFactory.echo("third")

    when:
    CompositeCommandSim composite1 = echo2 - echo1
    CompositeCommandSim composite2 = echo2 + echo3
    CompositeCommandSim composite3 = echo1 + (composite1 - composite2) + echo3

    then:
    composite3.commands == [echo1, echo2, echo3, echo1, echo2, echo3]
  }
}
