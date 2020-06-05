package com.lilly.cirrus.jenkinsdsl.sim

class CirrusCoreSpec extends CirrusDSLSpec {
  /**
   * Configures the supplied pipeline simulator with mocked previous builds.
   * @param pipelineSim The PipelineSim object
   * @param config A map of build number (int) mapped to its running status (boolean)
   */
  def setupPreviousBuild(JenkinsSim pipelineSim, Integer buildNumber = 0, Map<Integer, Boolean> config = [:]) {
    pipelineSim.withEnv('BUILD_NUMBER', buildNumber.toString())

    def builds = []
    config?.each {parentBuildNumber, isRunning ->
      WorkflowRunMock workflowRun = Mock() {
        getNumber() >> parentBuildNumber
        isBuilding() >> isRunning
      }
      builds << workflowRun
    }

    pipelineSim.currentBuild = [
      rawBuild: [
        parent: [
          builds: builds
        ]
      ]
    ]

    return builds
  }
}
