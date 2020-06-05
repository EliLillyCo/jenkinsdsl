package com.lilly.cirrus.jenkinsdsl.core.mock.block

import com.lilly.cirrus.jenkinsdsl.core.Block
import com.lilly.cirrus.jenkinsdsl.core.Scope

class BlockScopeMock extends Block {
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
