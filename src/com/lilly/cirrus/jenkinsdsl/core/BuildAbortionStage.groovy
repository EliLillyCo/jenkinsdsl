package com.lilly.cirrus.jenkinsdsl.core

class BuildAbortionStage extends Stage {
  private static final long serialVersionUID = 1L

  @Override
  Closure<?> getScript() {
    if (this.@script) return this.@script

    return { abortPreviousBuild() }
  }

  protected void abortPreviousBuild() {
    withJenkins {
      echo "Checking for previous running builds to abort..."

      def buildNumber = BUILD_NUMBER as int
      if (buildNumber > 1) {
        milestone(buildNumber - 1)
      }
      milestone(buildNumber)

      echo "Check for previous running builds complete. Proceeding to next stage, if any."
    }
  }


  static class Builder extends StageBuilder {
    private static final long serialVersionUID = 1L

    BuildAbortionStage stage = new BuildAbortionStage()

    static Builder create() {
      return new Builder()
    }
  }
}
