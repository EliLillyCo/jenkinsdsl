package com.lilly.cirrus.jenkinsdsl.core

class StashingShellStage extends ShellStage {
  private static final long serialVersionUID = -6044796481374617232L

  public static final String EXTENSION = ".txt"
  protected String stashName

  String getStashName() {
    return this.@stashName
  }

  void stashName(String stashName) {
    this.@stashName = stashName
  }

  @Override
  Closure<?> getScript() {
    if(this.@script) return this.@script

    return {
      withJenkins {
        String fileName = this.stashName + StashingShellStage.EXTENSION
        String newCommand = this.getStashSupportingCommand()
        sh newCommand
        stash name: this.stashName, includes: "${fileName}"
      }
    }
  }

  @Override
  Closure<?> getDryScript() {
    if (this.@dryScript) return this.@dryScript

    return {
      withJenkins {
        String fileName = this.stashName + StashingShellStage.EXTENSION
        String newCommand = this.getStashSupportingCommand()
        echo newCommand
        echo "stash name: ${this.stashName}, includes: ${fileName}"
      }
    }
  }

  protected String getStashSupportingCommand() {
    if(!command) {
      throw new CirrusPipelineException("A shell command must be configured for this stage.")
    }

    if(!stashName) {
      throw new CirrusPipelineException("A stash name must be configured for this stage.")
    }

    String fileName = stashName + EXTENSION
    return "${command} 2>&1 | tee ${fileName}"
  }

  static class Builder extends StageBuilder {
    private static final long serialVersionUID = -943953825583595L

    StashingShellStage stage = new StashingShellStage()

    static Builder create() {
      create(Builder)
    }

    Builder withCommand(String command) {
      stage.command(command)
      return this
    }

    Builder withStashName(String stashName) {
      stage.stashName(stashName)
      return this
    }
  }
}
