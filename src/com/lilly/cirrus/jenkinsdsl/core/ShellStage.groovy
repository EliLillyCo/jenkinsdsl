package com.lilly.cirrus.jenkinsdsl.core

class ShellStage extends Stage {
  private static final long serialVersionUID = -514091519265108718L
  protected String command

  void command(String command) {
    this.command = command
  }

  String getCommand() {
    return this.@command
  }

  @Override
  Closure<?> getScript() {
    if (this.@script) return this.@script

    return {
      withJenkins {
        sh this.command
      }
    }
  }

  @Override
  Closure<?> getDryScript() {
    if (this.@dryScript) return this.@dryScript

    return {
      withJenkins {
        echo this.command
      }
    }
  }

  static class Builder extends StageBuilder {
    private static final long serialVersionUID = -5439511380525583595L

    ShellStage stage = new ShellStage()

    static Builder create() {
      return new Builder()
    }

    Builder withCommand(String command) {
      stage.command(command)
      return this
    }
  }
}
