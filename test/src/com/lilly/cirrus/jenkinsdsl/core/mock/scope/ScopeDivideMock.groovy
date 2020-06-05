package com.lilly.cirrus.jenkinsdsl.core.mock.scope

import com.lilly.cirrus.jenkinsdsl.core.Scope

class ScopeDivideMock extends Scope {
  int divide(int a, int b) {
    return a / b
  }
}
