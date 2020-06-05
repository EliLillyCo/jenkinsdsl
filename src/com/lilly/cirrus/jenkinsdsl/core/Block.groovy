package com.lilly.cirrus.jenkinsdsl.core
/**
 * <p>
 * Block represents an execution block. A block will run on the master node if no docker information is
 * provided to it or will run on the specified docker if the docker information is provided to it.
 * </p>
 * <p>
 * The docker information is provided with dynamic fields and may not be seen in the static class definition.
 * If a block is chained with a hierarchy of parent parallelBlocks, then it will inherit the docker information from the
 * parent block. This essentially means that if an outer block specifies a docker to run its logic, then all the
 * inner parallelBlocks will reuse the same docker to run its logic unless an inner block overrides those fields.
 * This behavior is achieved through the dynamic fields implementation of the {@link Scope} class.
 * </p>
 * <p>
 * <b>Note</b>: A primitive type fields such as int, float, and boolean must not be statically defined in any subtype of
 * this {@link Scope} class including this class. Access to statically defined primitive field will break the current
 * {@link Scope} implementation. For instance, the {@code ok} field is not statically defined here.
 * </p>
 */
abstract class Block extends JenkinsScope {
  private static final long serialVersionUID = -7998716022939896766L

  protected Map<String, Closure> parallelTasks = [:]
  protected Map<String, Block> parallelBlocks = [:]
  protected Closure<?> finishingClosure

  protected def execute(Block parent, Closure<?> body) {
    this.expandScope(parent)

    if (!this.ok) return

    Exception exception = null
    try {
      DelegateFirstRunner.run this, body
    }
    catch(Exception e) {
      exception = new CirrusPipelineException(e)
    }

    if (this.@finishingClosure) {
      try { DelegateFirstRunner.run this, this.@finishingClosure, exception }
      catch (Exception e) { exception = e}
    }
    if (exception) throw exception

    this.reduceScope()
  }

  void onFinish(Closure<?> body) {
    this.@finishingClosure = body
  }

  protected def runBlock(String name, Closure<?> body) {
    if (!this.ok)
      return

    Block block = this.createBlock()
    if (name) {
      block.name = name
    }

    block.execute(this, body)
    return block
  }

  protected def execute(Stage stage) {
    if (!this.ok)
      return
    stage.execute(this)
    return stage
  }

  protected void runParallel(String name = "Parallel", Closure<?> body) {
    if (!this.ok) return

    Block enclosingBlock = createBlock()
    enclosingBlock.name = name

    enclosingBlock.execute(this, body) // Sets up tasks as runTask() gets called in the body
    enclosingBlock.executeParallelBlocks(this) // Executes the tasks in parallel
  }

  protected void executeParallelBlocks(Block parent) {
    expandScope(parent)

    // If runParallel() is invoked from within a subclass of Block, the parallel tasks are populated in the parent scope
    this.@parallelTasks = this.@parallelTasks?: parent?.@parallelTasks

    if (!this.ok || !this.@parallelTasks)
      return

    setupParallelBlocks()

    this.@parallelTasks.failFast = (this.failFast == false? false : true)
    withJenkins {
      stage(this.name) {
        parallel this.@parallelTasks
      }
    }

    tearDownParallelBlocks()
    reduceScope()
  }

  protected void runTask(String name, Closure<?> body) {
    if (!this.ok)
      return
    this.@parallelTasks[name] = body
  }

  protected void setupParallelBlocks() {
    this.@parallelTasks.each { name, body ->
      Block block = this.createBlock()
      block.name = name
      this.@parallelBlocks[name] = block

      body.delegate = block
      body.resolveStrategy = Closure.DELEGATE_FIRST

      block.expandScope(this)
    }
  }

  protected void tearDownParallelBlocks() {
    this.@parallelBlocks.values().each {it.reduceScope()}
  }

  /**
   * Factory method to create a Block in the subtypes. Override only when needed. By default it uses
   * the subclass' default constructor to initialize the object.
   */
  protected Block createBlock() {
    return this.getClass().getConstructor().newInstance()
  }
}
