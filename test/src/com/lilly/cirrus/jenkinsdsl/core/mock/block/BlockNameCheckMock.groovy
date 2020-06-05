package com.lilly.cirrus.jenkinsdsl.core.mock.block

import com.lilly.cirrus.jenkinsdsl.core.Block

class BlockNameCheckMock extends Block {
  Block createdBlock
  @Override
  protected Block createBlock() {
    createdBlock = super.createBlock()
    return createdBlock
  }
}
