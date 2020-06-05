package com.lilly.cirrus.jenkinsdsl.core.mock.scope

import com.lilly.cirrus.jenkinsdsl.core.Scope

class ScopeAddMock extends Scope {
  int add(int a, int b) {
    return a + b
  }
}
