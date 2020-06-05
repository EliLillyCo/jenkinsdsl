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

      def buildnum = env.BUILD_NUMBER.toInteger()
      def job = currentBuild?.rawBuild?.parent

      if (job && job.builds) {
        for (build in job.builds) {
          int buildId = build.getNumber()
          if (!build.isBuilding()) { continue }
          if (buildnum == build.getNumber().toInteger()) { continue }

          echo ">> Aborting build #${build.getNumber()}"
          build.doKill()
        }
      }

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
