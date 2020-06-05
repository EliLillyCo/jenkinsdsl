package com.lilly.cirrus.jenkinsdsl.core.mock.stage

import com.lilly.cirrus.jenkinsdsl.core.Scope
import com.lilly.cirrus.jenkinsdsl.core.Stage

class StageScopeMock extends Stage {
  @Override
  void expandScope(Scope parent) {
    super.expandScope(parent)
    this.scopeExpanded = true
  }

  @Override
  void reduceScope() {
    super.reduceScope()
    this.scopeReduced = true
  }
}
