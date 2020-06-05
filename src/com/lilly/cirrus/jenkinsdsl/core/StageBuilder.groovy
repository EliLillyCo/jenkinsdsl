package com.lilly.cirrus.jenkinsdsl.core

abstract class StageBuilder implements Serializable {
  private static final long serialVersionUID = 3028459473012163472L

  static <T extends StageBuilder> T create(Class<T> clazz) {
    return clazz.getConstructor().newInstance()
  }

  StageBuilder withName(String name) {
    stage.name name
    return this
  }

  StageBuilder withBody(Closure<?> closure) {
    stage.body closure
    return this
  }

  StageBuilder withWhen(Closure<?> closure) {
    stage.when closure
    return this
  }

  StageBuilder withPreScript(Closure<?> closure) {
    stage.preScript closure
    return this
  }

  StageBuilder withDryScript(Closure<?> closure) {
    stage.dryScript closure
    return this
  }

  StageBuilder withScript(Closure<?> closure) {
    stage.script closure
    return this
  }

  StageBuilder withPostScript(Closure<?> closure) {
    stage.postScript closure
    return this
  }

  StageBuilder withStageConfig(StageConfig stageConfig) {
    stage.stageConfig(stageConfig)
    return this
  }

  Stage build() {
    return stage
  }

  /**
   * Subtype should create a concrete stage as a field and return it using this method.
   */
  protected abstract Stage getStage()
}
