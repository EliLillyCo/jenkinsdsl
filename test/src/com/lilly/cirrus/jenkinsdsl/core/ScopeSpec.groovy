package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.scope.ScopeAddMock
import com.lilly.cirrus.jenkinsdsl.core.mock.scope.ScopeLocalMock
import com.lilly.cirrus.jenkinsdsl.core.mock.scope.ScopeRemainderMock
import com.lilly.cirrus.jenkinsdsl.core.mock.scope.ScopeSubMock

class ScopeSpec extends CirrusDSLSpec {

  def "when a scope is created, the fields should be initialized"() {
    when: "a new scope is created"
    Scope scope = new Scope()

    then: "the parent scope is not set"
    scope.parent == null

    and: "no dynamic fields are created"
    scope.properties.isEmpty()
  }

  def "expanding a scope should attach the parent scope to the current scope"() {
    given: "A scope and a parent scope object"
    Scope scope = new Scope()
    Scope parent = new Scope()

    when: "a new scope is created"
    scope.expandScope(parent)

    then: "the parent of scope is null"
    scope.parent == parent
  }

  def "reducing a scope should remove the parent scope from the current scope"() {
    given: "An expanded scope object"
    Scope scope = new Scope()
    Scope parent = new Scope()
    scope.expandScope(parent)


    when: "the scope is reduced"
    scope.reduceScope()

    then: "the scope should not include the parent scope"
    !scope.parent
  }

  def "root should return the root of a scope chain"() {
    when: "scopes are chained"
    Scope scope = new Scope()
    Scope scope1 = new Scope()
    Scope scope2 = new Scope()
    scope.parent = scope1
    scope1.parent = scope2

    then: "the root scope can be correctly retrieved"
    scope.root() == scope2
    scope1.root() == scope2
  }


  def "scope should support dynamic fields"() {
    given: "A new scope"
    Scope scope = new Scope()

    when: "dynamic fields are added"
    scope.firstName = "John"
    scope.lastName = "Doe"

    then: "they can be accessed"
    scope.firstName == "John"
    scope.lastName == "Doe"

    and: "they are stored as dynamic fields"
    scope.properties.firstName == "John"
    scope.properties.lastName == "Doe"
  }

  def "a scope should first access its fields before creating dynamic variables"() {
    given: "A subclass of Scope"
    ScopeSubMock scope = new ScopeSubMock()

    when: "assigning a value to an existing field"
    scope.dummyField = "Test"

    then: "should change the field"
    scope.dummyField == "Test"

    and: "not add a new dynamic variable"
    !scope.properties
  }

  def "a child scope in a hierarchy should be able to access variables defined in parent scopes"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    when: "dynamic fields are defined in parent scopes"
    grandParent.lastName = "Doe"
    parent.firstName = "John"

    then: "the child scope should be able to read them"
    child.lastName == "Doe"
    child.firstName == "John"
  }

  def "a child scope should not be able to directly modify variables defined in parent scopes"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    when: "variables are defined in parent scopes"
    grandParent.lastName = "Doe"
    parent.firstName = "John"

    and: "changing parent variables in child scope"
    child.firstName = "Jack"
    child.lastName = "Rabbit"

    then: "the changes should be local to child scope"
    child.firstName == "Jack"
    child.lastName == "Rabbit"

    and: "the parents have no side effect"
    grandParent.lastName == "Doe"
    parent.firstName == "John"
  }

  def "hasLocalProperty should know about local properties"() {
    given: "a scope with local property, first"
    ScopeLocalMock scope = new ScopeLocalMock()

    when: "hasLocalProperty() is called for the first property"
    boolean found = scope.hasLocalProperty("first")

    then: "it should be found"
    found
  }

  def "hasLocalProperty should know about local fields"() {
    given: "a scope with a local field, last"
    ScopeLocalMock scope = new ScopeLocalMock()

    when: "hasLocalProperty() is called for the second field"
    boolean found = scope.hasLocalProperty("last")

    then: "it should be found"
    found
  }

  def "the top most scope containing the given field should be returned when asked for root"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    when: "the same variable is defined in multiple scopes"
    grandParent.state = "New York"

    parent.state = "Illinois"
    parent.city = "Chicago"

    child.state = "Indiana"
    child.city = "Indianapolis"
    child.street = "Harding"

    then: "root should always return the top-most scope defining the given field"
    child.root("state") == grandParent
    child.root("city") == parent
    child.root("street") == child

    and: "root should be null if the given field is not defined in any scopes"
    child.root("streetNumber") == null
  }

  def "the closest scope containing the given field should be returned when asked for the nearest"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    when: "the same variable is defined in multiple scopes"
    grandParent.state = "Indiana"
    grandParent.city = "Indianapolis"
    grandParent.street = "Harding"

    parent.state = "Illinois"
    parent.city = "Chicago"

    child.state = "New York"

    then: "nearest should always return the closed scope defining the given field"
    child.nearest("state") == child
    child.nearest("city") == parent
    child.nearest("street") == grandParent

    and: "nearest should be null if the given field is not defined in any scopes"
    child.nearest("streetNumber") == null
  }

  def "all the scopes containing the given field should be returned when asked for all"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    when: "the same variable is defined in multiple scopes"
    grandParent.state = "New York"

    parent.state = "Illinois"
    parent.city = "Chicago"

    child.state = "Indiana"
    child.city = "Indianapolis"
    child.street = "Harding"

    then: "all should return the all scopes defining the given field"
    child.all("state") == [child, parent, grandParent]
    child.all("city") == [child, parent]
    child.all("street") == [child]

    and: "all should return an empty list if the given field is not defined in any scopes"
    child.all("streetNumber") == []
  }

  def "the closest scope containing the given field should be set correctly"() {
    given: "A hierarchy of scopes"
    Scope child = new Scope()
    Scope parent = new Scope()
    Scope grandParent = new Scope()
    child.expandScope(parent)
    parent.expandScope(grandParent)

    and: "the same variable is defined in multiple scopes"
    grandParent.state = "Indiana"
    grandParent.city = "Indianapolis"
    grandParent.street = "Harding"

    parent.state = "Illinois"
    parent.city = "Chicago"

    child.state = "New York"

    when: "the nearest variable is set to a new value"
    child.city = "Boston"

    then: "the field should contain the updated value"
    child.city == "Boston"
  }

  def "setNearestProperty should throw an exception when a non-existant property is provided"() {
    given: "A scope"
    Scope child = new Scope()

    when: "setNearestproperty is specified for a non-existant property"
    child.setNearestProperty("streetNumber", "12345")

    then: "setNearestProperty should throw an exception if the given field is not defined in any scopes"
    thrown(NoSuchFieldException)
  }

  def "scope should allow runtime method inheritance through scope chaining"() {
    given: "A parent and subclasses scope-chained together"
    Scope grandParent = new ScopeRemainderMock()
    Scope parent = new Scope()
    Scope child = new ScopeAddMock()
    child.parent = parent
    parent.parent = grandParent

    when: "chained-super type methods are invoked in a subtype"
    int result = child.add(2, child.divide(4, child.remainder(6, 4)))

    then: "the methods correctly get delegated to the super types for execution"
    result == 4
  }

  def "scope should throw an exception if a given method is not found in the chained scope"() {
    given: "A parent and subclasses scope-chained together"
    Scope grandParent = new ScopeRemainderMock()
    Scope parent = new Scope()
    Scope child = new ScopeAddMock()
    child.parent = parent
    parent.parent = grandParent

    when: "chained-super type methods are invoked in a subtype with a missing method"
    int result = child.aggregate(2, child.divide(4, child.remainder(6, 4)))

    then: "an exception is thrown for the missing method"
    thrown(MissingMethodException)
  }

  def "scope should allow runtime method inheritance through scope chaining for delegated closure"() {
    given: "A parent and subclasses scope-chained together and a closure"
    Scope grandParent = new ScopeRemainderMock()
    Scope parent = new Scope()
    Scope child = new ScopeAddMock()
    child.parent = parent
    parent.parent = grandParent
    Closure<Integer> closure = {
      add(2, divide(4, remainder(6, 4)))
    }

    when: "chained-super type methods are invoked in a subtype through closure"
    int result = DelegateFirstRunner.run child, closure

    then: "the methods correctly get delegated to the super types for execution"
    result == 4
  }

  def "should be able to check local property"() {
    when: "a scope with local property is created"
    Scope scope = new Scope()
    scope.name = "John"

    then: "local property can be checked"
    scope.hasLocalProperty("name")
    scope.getLocalProperty("name") == "John"
    scope.name == "John"
  }
}
