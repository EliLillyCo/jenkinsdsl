package com.lilly.cirrus.jenkinsdsl.utility

import com.cloudbees.groovy.cps.NonCPS

class Exceptions {
  @NonCPS
  static String printStackTrace(Exception e) {
    StringWriter sw = new StringWriter()
    PrintWriter pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    return sw.toString()
  }
}
