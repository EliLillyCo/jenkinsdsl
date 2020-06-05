package com.lilly.cirrus.jenkinsdsl.core

import com.cloudbees.groovy.cps.NonCPS

/**
 * Encapsulates output details for a build. Classes can use this to perform
 * tasks that need to be done consistently with output from a Jenkins build,
 * for example, output can often be stored for writing to a file and also
 * sent to the console log in Jenkins.
 */
class BuildOutput {
  def resultsOutput
  def pipeline
  
  @NonCPS
  def getResultsOutput() {
    if (resultsOutput == null) {
      resultsOutput = ''
    }
    resultsOutput
  }

  void appendWithEcho(message) {
    append(message)
    pipeline.echo(message)
  }

  void append(message) {
    resultsOutput = getResultsOutput() + message
  }

  /**
   * Logs error message to results output and re-throws the exception.
   * 
   * @param exception the exception to be logged and re-thrown
   * @throws the exception that was provided as an argument
   */
  void appendAndThrow(Exception exception) {
    appendWithEcho("${exception.message}\n")
    throw exception
  }

  void write(fileName) {
    pipeline.writeFile(file: fileName, text: getResultsOutput(), encoding: 'UTF-8')
  }
}
