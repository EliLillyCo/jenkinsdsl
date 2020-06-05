package com.lilly.cirrus.jenkinsdsl.sim

import org.spockframework.lang.Wildcard

class CommandSim {
  protected def type
  protected def arguments
  protected int executionOrder = -1
  protected Closure<?> body

  def getType() {
    return type
  }

  void setType(def type) {
    this.type = type
  }

  def getArguments() {
    return arguments
  }

  void setArguments(def arguments) {
    this.arguments = arguments
  }

  int getExecutionOrder() {
    return executionOrder
  }

  void setExecutionOrder(int executionOrder) {
    this.executionOrder = executionOrder
  }

  Closure<?> getBody() {
    return body
  }

  void setBody(Closure<?> body) {
    this.body = body
  }

  CommandSim plus(CommandSim command) {
    CompositeCommandSim composite = new CompositeCommandSim()
    composite + this
    composite + command
    return composite
  }

  CommandSim minus(CommandSim command) {
    CompositeCommandSim composite = new CompositeCommandSim()
    composite - this
    composite - command
    return composite
  }

  @Override
  boolean equals(o) {
    // Note that this implementation intentionally violates the symmetry contract of equivalence relation
    if (o == null) return false
    if (this.is(o)) return true
    if (o.is(Wildcard.INSTANCE)) return true

    if (getClass() != o.class) return false
    CommandSim command = (CommandSim) o


    if (!equalsPolymorphic(this.type, command.type)) return false
    if (!equalsPolymorphic(this.arguments, command.arguments)) return false

    return true
  }

  boolean equalsPolymorphic(def thisValue, def thatValue) {
    if (thatValue == null) return thisValue == null
    if (thatValue.is(thisValue)) return true
    if (thatValue.is(Wildcard.INSTANCE)) return true
    if (thatValue instanceof Closure) return thatValue(thisValue)

    if (thisValue instanceof List && thatValue instanceof List) return equalsList(thisValue, thatValue)
    if (thisValue instanceof Map && thatValue instanceof Map) return equalsMap(thisValue, thatValue)

    return thisValue == thatValue
  }

  boolean equalsList(List thisList, List thatList) {
    if (thisList.size() != thatList.size()) return false

    for (int i = 0; i < thisList.size(); ++i) {
      if (!equalsPolymorphic(thisList.get(i), thatList.get(i))) return false
    }

    return true
  }

  boolean equalsMap(Map thisMap, Map thatMap) {
    if (thisMap.size() != thatMap.size()) return false

    boolean match = true
    thisMap.each { key, thisMapValue ->
      def thatMapValue = thatMap.get(key)
      if (!equalsPolymorphic(thisMapValue, thatMapValue)) {
        match = false
        return
      }
    }
    return match
  }

  @Override
  int hashCode() {
    int result
    result = (type != null ? type.hashCode() : 0)
    result = 31 * result + (arguments != null ? arguments.hashCode() : 0)
    return result
  }

  @Override
  String toString() {
    StringBuilder builder = new StringBuilder()

    if (type) builder.append(type)
    else builder.append(this.class.simpleName)

    if (arguments instanceof Map) builder.append(' ')
    else return builder.toString()

    int i = 0
    for (Map.Entry<String, Object> entry: arguments.entrySet()) {
      builder.append(entry.key)
      builder.append(': ')
      builder.append(entry.value)

      if (i < arguments.size() - 1) builder.append(', ')
      ++i
    }

    return builder.toString()
  }

}
