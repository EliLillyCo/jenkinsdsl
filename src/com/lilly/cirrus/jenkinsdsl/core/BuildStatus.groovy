package com.lilly.cirrus.jenkinsdsl.core

class BuildStatus implements Serializable {
  private static final long serialVersionUID = 1L
  public static final String SUCCESS = 'SUCCESS'
  public static final String ABORTED = 'ABORTED'
  public static final String FAILURE = 'FAILURE'


  public String color
  public String icon
  public String text

  static BuildStatus createSuccess() {
    return new BuildStatus(color: 'good', icon: ':white_check_mark:', text: SUCCESS)
  }

  static BuildStatus createAborted() {
    return new BuildStatus(color: 'warning', icon: ':warning:', text: ABORTED)
  }

  static BuildStatus createFailure() {
    return new BuildStatus(color: 'danger', icon: ':octagonal_sign:', text: FAILURE)
  }

  static BuildStatus createOther(String text) {
    return new BuildStatus(color: 'danger', icon: ':octagonal_sign:', text: text)
  }

  static BuildStatus fromPipeline(def pipeline) {
    if (!pipeline.currentBuild.result) return createSuccess()
    if (pipeline.currentBuild.result == 'ABORTED') return createAborted()
    return createOther(pipeline.currentBuild.result as String)
  }

  boolean equals(o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    BuildStatus that = (BuildStatus) o

    if (color != that.color) return false
    if (icon != that.icon) return false
    if (text != that.text) return false

    return true
  }

  int hashCode() {
    int result
    result = color.hashCode()
    result = 31 * result + icon.hashCode()
    result = 31 * result + text.hashCode()
    return result
  }
}
