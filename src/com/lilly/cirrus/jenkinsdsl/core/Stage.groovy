package com.lilly.cirrus.jenkinsdsl.core


import com.lilly.cirrus.jenkinsdsl.utility.Exceptions

class Stage extends JenkinsScope {
  private static final long serialVersionUID = 7819180431985464369L

  protected Closure<?> body
  protected Closure<?> when
  protected Closure<?> preScript
  protected Closure<?> dryScript
  protected Closure<?> script
  protected Closure<?> postScript

  protected StageConfig stageConfig

  void execute(Scope scope) {
    this.expandScope(scope)

    if (!this.ok) return

    if (!this.pod) throw new CirrusPipelineException("A stage must run inside a pod. Please use the runPod {} dsl method.")

    runWithArgs this, getBody()

    if (!runWithArgs(this, getWhen())) return

    if (!this.@stageConfig) stageConfig(StageConfig.create(name: name))
    this.stages[name] = this.@stageConfig // stages defined at EntryPoint.start()

    withJenkins {

        // timeout(time:60, unit:'SECONDS') { }
        echo " Running [${this.name}] stage"

        stage(this.name) {


          if (this.dryRun) runWithArgs(this, getDryScript())
          else runScript()
          // if (this.name == 'Prepare Enterprise Image')
          //   {
          //     echo " Sleeping 30000000 sec in [${this.name}] stage"
          //     // sleep(30000000)
          //   }
        }
    }

    this.reduceScope()
  }

  protected void runScript() {
    Closure<?> preScriptClosure = this.getPreScript()
    Closure<?> scriptClosure = this.getScript()
    if (!scriptClosure && !preScriptClosure) {
      throw new CirrusPipelineException("A Stage must have either a script or preScript closure specified at the bare minimum.")
    }
    try {
      runWithArgs this, preScriptClosure
      runWithArgs this, scriptClosure
      runWithArgs this, getPostScript()
    }
    catch(Exception e) {
      runWithArgs this, getPostScript(), e
      throw e
    }
  }

  protected def runWithArgs(def receiver, Closure<?> closure, def args = null, def defaultValue = true) {
    if (closure) return DelegateFirstRunner.run(receiver, closure, args)
    return defaultValue
  }

  /**
   * Subtype dependency injection hook for the when closure.
   */
  Closure<?> getWhen() {
    return this.@when
  }

  void when(Closure<?> body) {
    this.@when = body
  }

  /**
   * Subtype dependency injection hook for the preScript closure.
   */
  Closure<?> getPreScript() {
    return this.@preScript
  }

  void preScript(Closure<?> body) {
    this.@preScript = body
  }

  /**
   * Subtype dependency injection hook for the dryScript closure.
   */
  Closure<?> getDryScript() {
    if (!this.@dryScript) this.dryScript {
      echo "Dry running the [${this.name}] stage ..."
    }

    return this.@dryScript
  }

  void dryScript(Closure<?> body) {
    this.@dryScript = body
  }

  /**
   * Subtype dependency injection hook for the script closure.
   */
  Closure<?> getScript() {
    return this.@script
  }

  void script(Closure<?> body) {
    this.@script = body
  }

  /**
   * Subtype dependency injection hook for the postScript closure.
   */
  Closure<?> getPostScript() {
    if (!this.@postScript) this.postScript { Exception e ->
      withJenkins {
        if (fileExists(this.stageConfig.resultsOutputFile))
          stash name: this.stageConfig.stashName, includes: this.stageConfig.resultsOutputFile
      }

      if (e) {
        String trace = Exceptions.printStackTrace(e)
        withJenkins { echo trace }
        this.stageConfig.failed = true
      }
    }

    return this.@postScript
  }

  void postScript(Closure<?> body) {
    this.@postScript = body
  }

  /**
   * Subtype dependency injection hook for the body closure.
   */
  Closure<?> getBody() {
    return this.@body
  }

  void body(Closure<?> body) {
    this.@body = body
  }

  StageConfig getStageConfig() {
    return this.@stageConfig
  }

  void stageConfig(StageConfig stageConfig) {
    this.@stageConfig = stageConfig
    if (stageConfig?.name) this.@name = stageConfig.name
  }

  /**
   * Writes the given text using the bash's echo command and tees the result to the results output file specified in the
   * StageConfig.
   */
  void writeWithEcho(String text) {
    withJenkins { sh "echo '${text}' 2>&1 | tee -a ${this.stageConfig.resultsOutputFile}" }
  }

  /**
   * Writes the given text using the bash's echo command to the results output file specified in the StageConfig and
   * the pipeline errors out with the given text.
   */
  void writeWithError(String text) {
    withJenkins {
      sh "echo '${text}' >> ${this.stageConfig.resultsOutputFile}"
      error text
    }
  }

  static class Builder extends StageBuilder {
    private static final long serialVersionUID = -4439511380525583595L

    Stage stage = new Stage()

    static Builder create() {
      create(Builder)
    }
  }
}
