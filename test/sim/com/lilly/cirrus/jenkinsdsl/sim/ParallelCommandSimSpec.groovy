package com.lilly.cirrus.jenkinsdsl.sim

class ParallelCommandSimSpec extends CirrusDSLSpec {
  ParallelCommandSim parallelCommandSim

  def arguments = ["Run Test": {sh "npm test"}, "Run Lint": {sh "npm run lint"}]
  int order = 1

  def setup() {
    parallelCommandSim = new ParallelCommandSim()
    parallelCommandSim.setArguments(arguments)
    parallelCommandSim.setExecutionOrder(order)
  }

  def "a command should be equal to itself"() {
    expect:
    parallelCommandSim == parallelCommandSim
    new ParallelCommandSim() == new ParallelCommandSim()
    new ParallelCommandSim(arguments: arguments) == new ParallelCommandSim(arguments: arguments)
    new ParallelCommandSim(arguments: arguments) == new ParallelCommandSim(arguments: arguments.clone())
    new ParallelCommandSim(arguments: [a: {}, b: {}]) == new ParallelCommandSim(arguments: [b: {}, a: {}])
  }

  def "equal command should have equal hashCode"() {
    expect:
    parallelCommandSim.hashCode() == parallelCommandSim.hashCode()
    new ParallelCommandSim().hashCode() == new ParallelCommandSim().hashCode()
    new ParallelCommandSim(arguments: arguments).hashCode() == new ParallelCommandSim(arguments: arguments).hashCode()
    new ParallelCommandSim(arguments: arguments).hashCode() == new ParallelCommandSim(arguments: arguments.clone()).hashCode()
  }

  def "a command should not be equal to different types or different commands"() {
    expect:
    new ParallelCommandSim(arguments: null) != new ParallelCommandSim(arguments: [a: {}])
    new ParallelCommandSim(arguments: [a: {}]) != new ParallelCommandSim(arguments: null)
    new ParallelCommandSim(arguments: [a: {}, b: {}]) != new ParallelCommandSim(arguments: [b: {}])
    new ParallelCommandSim(arguments: [a: {}, b: {}]) != new ParallelCommandSim(arguments: [a: {}, c: {}])
  }

  def "an empty command should still have a string representation"() {
    expect:
    new ParallelCommandSim().toString() == "parallel"
    new ParallelCommandSim(arguments: null).toString() == "parallel"
  }

  def "a command should properly include arguments in its string representation"() {
    expect:
    new ParallelCommandSim(arguments: [failFast: true, a: {}, b: {}]).toString() == "parallel failFast: true, a: {...}, b: {...}"
  }
}
