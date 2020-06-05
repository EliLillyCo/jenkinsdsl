package com.lilly.cirrus.jenkinsdsl.sim

class CirrusDSLSpecSpec extends CirrusDSLSpec {
  def "createPipelineSim should create a PipelineSim object"() {
    expect:
    createJenkinsSim() instanceof JenkinsSim
  }
}
