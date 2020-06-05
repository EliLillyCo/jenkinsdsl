package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec

class CirrusPipelineExceptionSpec extends CirrusDSLSpec {

  def "should be able to create the exception with a message"() {
    when: "calling constructor with message"
    CirrusPipelineException exception = new CirrusPipelineException("error")

    then: "the exception should set the message correctly"
    exception.message == "error"
  }

  def "should be able to create the exception with a message and throwable"() {
    when: "calling constructor with message and a throwable"
    Exception throwable = new Exception()
    CirrusPipelineException exception = new CirrusPipelineException("another error", throwable)

    then: "the exception should set the message and throwable correctly"
    exception.cause == throwable
  }

  def "should be able to create the exception with a throwable"() {
    when: "calling constructor with message and a throwable"
    Exception throwable = new Exception("error")
    CirrusPipelineException exception = new CirrusPipelineException(throwable)

    then: "the exception should set the throwable correctly"
    exception.cause == throwable
  }

  def "should be able to get complete stack trace as a string"() {
    given: "An exception with a cause"
    String throwMsg = "##THROWABLE##"
    String errorMsg = "##ERROR##"
    Exception throwable = new CirrusPipelineException(throwMsg)
    CirrusPipelineException exception = new CirrusPipelineException(errorMsg, throwable)

    when: "calling getStackTraceAsString on the exception"
    String stackTrace = exception.getStackTraceAsString()

    then: "the exception should return stack trace with the cause"
    stackTrace.contains(throwMsg)
    stackTrace.contains(errorMsg)
  }

  def "should be able to get stack trace even with a cause"() {
    given: "An exception without a cause"
    String errorMsg = "##ERROR##"
    CirrusPipelineException exception = new CirrusPipelineException(errorMsg)

    when: "calling getStackTraceAsString() on the exception"
    String stackTrace = exception.getStackTraceAsString()

    then: "the exception should return stack trace"
    stackTrace.contains(errorMsg)
  }
}
