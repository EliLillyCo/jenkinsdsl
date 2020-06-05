package com.lilly.cirrus.jenkinsdsl.sim

interface WorkflowRunMock {
  boolean isBuilding()
  int getNumber()
  void doKill()
}
