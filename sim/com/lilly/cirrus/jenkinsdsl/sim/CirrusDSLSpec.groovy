package com.lilly.cirrus.jenkinsdsl.sim

import com.lilly.cirrus.jenkinsdsl.core.EntryPoint
import com.lilly.cirrus.jenkinsdsl.core.Scope
import com.lilly.cirrus.jenkinsdsl.core.Stage
import spock.lang.Specification

class CirrusDSLSpec extends Specification {
  void setup() {
    EntryPoint.cleanup()
  }

  void cleanup() {
    EntryPoint.cleanup()
  }

  JenkinsSim createJenkinsSim() {
    return new JenkinsSim()
  }

  void setupStage(Stage stage, JenkinsSim jenkins) {
    stage.ok = true
    stage.jenkins = jenkins
    stage.env = jenkins.env
  }

  void setupScope(Scope scope, JenkinsSim jenkins) {
    scope.ok = true
    scope.jenkins = jenkins
    scope.env = jenkins.env
    scope.pod = 'pod'
    scope.stages = [:]
  }
}
