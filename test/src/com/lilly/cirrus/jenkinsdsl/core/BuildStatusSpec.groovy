package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec

class BuildStatusSpec extends CirrusDSLSpec {
  def "should be able to create build status objects based on jenkins pipeline status"(def pipeline, BuildStatus status) {
    expect:
    BuildStatus.fromPipeline(pipeline) == status

    where:
    pipeline                              | status
    [currentBuild: [result: 'ABORTED']]   | new BuildStatus(color: 'warning', icon: ':warning:', text: 'ABORTED')
    [currentBuild: [result: 'ERROR']]     | new BuildStatus(color: 'danger', icon: ':octagonal_sign:', text: 'ERROR')
    [currentBuild: []]                    | new BuildStatus(color: 'good', icon: ':white_check_mark:', text: 'SUCCESS')
  }
}
