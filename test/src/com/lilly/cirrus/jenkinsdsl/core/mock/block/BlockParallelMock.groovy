package com.lilly.cirrus.jenkinsdsl.core.mock.block

import com.lilly.cirrus.jenkinsdsl.core.Block
import com.lilly.cirrus.jenkinsdsl.core.Scope

class BlockParallelMock extends Block {
  @Override
  protected void setupParallelBlocks() {
    setupParallelBlocksExecuted = true
    super.setupParallelBlocks()
  }

  @Override
  protected void tearDownParallelBlocks() {
    tearDownParallelBlocksExecuted = true
    super.tearDownParallelBlocks()
  }

  @Override
  void expandScope(Scope parent) {
    expandScopeExecuted = true
    super.expandScope(parent)
  }

  @Override
  void reduceScope() {
    reduceScopeExecuted = true
    super.reduceScope()
  }
}
