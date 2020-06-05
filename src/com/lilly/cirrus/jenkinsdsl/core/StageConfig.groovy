package com.lilly.cirrus.jenkinsdsl.core

class StageConfig {
  static StageConfig create(Map config) {
    StageConfig stage = new StageConfig()
    stage.name = config.name ? config.name : stage.name
    stage.failed = config.failed ? config.failed : stage.failed
    stage.warn = config.warn ? config.warn : stage.warn
    stage.includeInComment = config.includeInComment ? config.includeInComment : stage.includeInComment
    stage.stashName = config.stashName ? config.stashName : stage.stashName
    stage.resultsPreambleFile = config.resultsPreambleFile ? config.resultsPreambleFile : stage.resultsPreambleFile
    stage.resultsOutputFile = config.resultsOutputFile ? config.resultsOutputFile : stage.resultsOutputFile
    stage.resultsOutputType = config.resultsOutputType ? config.resultsOutputType : stage.resultsOutputType
    stage.resultsOutputTitle = config.resultsOutputTitle ? config.resultsOutputTitle : "${stage.name} Results"
    return stage
  }

  String id = UUID.randomUUID().toString()
  String name = "Unnamed Stage ${id}"
  boolean failed = false
  boolean warn = false
  boolean includeInComment = false
  String stashName = "stash-${id}"
  String resultsPreambleFile = ''
  String resultsOutputFile = "resultsOutput-${id}.log"
  String resultsOutputType = 'text/plain'
  String resultsOutputTitle

  private StageConfig() {}
}
