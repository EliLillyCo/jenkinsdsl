package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec

class DelegateFirstRunnerSpec extends CirrusDSLSpec {
  class DelegateTest extends Scope {
    protected String name

    void mutateName(String text) {
      name = text
    }
  }

  def "running a closure with delegate first strategy should be able to call methods and fields of delegate"() {
    given: "A delegate with an initial value and a closure that can modify the initial value"
    DelegateTest delegate = new DelegateTest(name: "Initial")
    Closure<?> closure = { name ->
      mutateName(name)
    }

    when: "the closure is run on the delegate"
    DelegateFirstRunner.run delegate, closure, "Changed"

    then: "the delegate initial value must change"
    delegate.name == "Changed"
  }

  def "a closure for run should also work with empty arguments"() {
    given: "A delegate with an initial value and a closure that can modify the initial value"
    DelegateTest delegate = new DelegateTest(name: "Initial")
    Closure<?> closure = {
      mutateName("Changed")
    }

    when: "the closure is run on the delegate"
    DelegateFirstRunner.run delegate, closure

    then: "the delegate initial value must change"
    delegate.name == "Changed"
  }

  def "running an empty closure should return null result"() {
    given: "a mock delegate"
    Scope scope = new Scope()

    when: "run is called with a null closure"
    def value = DelegateFirstRunner.run scope, null

    then: "it should return null"
    value == null
  }

  def "setup should be able to handle null closure"() {
    given: "a mock delegate"
    Scope scope = new Scope()

    when: "setup is called with a null closure"
    def closure = DelegateFirstRunner.setup scope, null

    then: "it should return null"
    closure == null
  }
}
