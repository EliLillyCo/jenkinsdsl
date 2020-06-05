package com.lilly.cirrus.jenkinsdsl.core

class CirrusPipelineException extends RuntimeException {
  private static final long serialVersionUID = -3365825374878524018L

  CirrusPipelineException(String message) {
    super(message)
  }

  CirrusPipelineException(String message, Throwable cause) {
    super(message, cause)
  }

  CirrusPipelineException(Throwable cause) {
    super(cause)
  }

  /**
   * Returns the stacktrace of the exception as a String.
   */
  String getStackTraceAsString() {
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    PrintStream printer = new PrintStream(out, true)
    this.printStackTrace(printer)
    return out.toString()
  }
}
