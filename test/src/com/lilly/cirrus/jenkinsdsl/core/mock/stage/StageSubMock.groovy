package com.lilly.cirrus.jenkinsdsl.core.mock.stage

import com.lilly.cirrus.jenkinsdsl.core.Stage

class StageSubMock extends Stage {
  @Override
  Closure<?> getWhen() {
    return {
      whenExecuted = true
      return whenExecuted
    }
  }

  @Override
  Closure<?> getPreScript() {
    return {
      preScriptExecuted = true
    }
  }

  @Override
  Closure<?> getDryScript() {
    return {
      dryScriptExecuted = true
    }
  }

  @Override
  Closure<?> getScript() {
    return {
      scriptExecuted = true
    }
  }

  @Override
  Closure<?> getPostScript() {
    return {
      postScriptExecuted = true
    }
  }

  @Override
  Closure<?> getBody() {
    return {
      bodyExecuted = true
    }
  }
}
