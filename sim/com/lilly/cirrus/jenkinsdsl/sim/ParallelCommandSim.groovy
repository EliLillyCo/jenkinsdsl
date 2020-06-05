package com.lilly.cirrus.jenkinsdsl.sim

import org.spockframework.lang.Wildcard

/**
 * <p>
 * Mock for the parallel() jenkins command.
 * </p>
 * <b>Known Limitation:</b><br/>
 * Since we cannot compare two closures for value-based equality, the
 * equality comparison of two parallel commands are limited to the
 * comparision of the keys associated with them.
 */
class ParallelCommandSim extends CommandSim {

  boolean equals(o) {
    // Note that this implementation intentionally violates the symmetry contract of equivalence relation
    if (o == null) return false
    if (this.is(o)) return true
    if (o.is(Wildcard.INSTANCE)) return true

    if (getClass() != o.class) return false
    ParallelCommandSim command = (ParallelCommandSim) o

    if (equalsPolymorphic(this.arguments, command.arguments)) return true

    if (arguments && !command.arguments) return false
    if (!arguments && command.arguments) return false
    if (arguments.size() != command.arguments.size()) return false
    for (def entry in arguments) {
      if (!command.arguments.containsKey(entry.key)) return false
    }

    return true
  }

  int hashCode() {
    int result = 1

    if (!arguments) return result

    for (def entry in arguments) {
      result = 31 * result + entry.key.hashCode()
    }

    return result
  }

  @Override
  String toString() {
    StringBuilder builder = new StringBuilder()
    builder.append("parallel")

    if (!arguments) return builder.toString()
    builder.append(' ')

    int i = 0
    for (Map.Entry<String, Object> entry: arguments.entrySet()) {
      builder.append(entry.key)
      builder.append(': ')

      if (entry.value instanceof Closure) builder.append('{...}')
      else builder.append(entry.value)

      if (i < arguments.size() - 1) builder.append(', ')
      ++i
    }

    return builder.toString()
  }
}
