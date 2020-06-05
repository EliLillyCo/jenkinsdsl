package com.lilly.cirrus.jenkinsdsl.sim

class CompositeCommandSim extends CommandSim {
  protected def commands = []

  @Override
  CommandSim minus(CommandSim command) {
    if (command instanceof CompositeCommandSim) {
      def newCommands = []
      newCommands.addAll(command.commands)
      newCommands.addAll(this.commands)
      commands = newCommands
    }
    else {
      commands.add(0, command)
    }
    return this
  }

  @Override
  CommandSim plus(CommandSim command) {
    if (command instanceof CompositeCommandSim) {
      def newCommands = []
      newCommands.addAll(this.commands)
      newCommands.addAll(command.commands)
      commands = newCommands
    }
    else {
      commands.add(command)
    }
    return this
  }

}
