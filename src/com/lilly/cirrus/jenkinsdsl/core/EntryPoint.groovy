package com.lilly.cirrus.jenkinsdsl.core

/**
 * <p>
 * This class represents an abstraction for a pipeline entry point. The script in vars folder will call the
 * {@link EntryPoint#start} method on subtypes of this class.
 * </p>
 *
 * <p>
 * <b>Note</b>: A primitive type fields such as int, float, and boolean must not be statically defined in any subtype of
 * this {@link Scope} class including this class. Access to statically defined primitive field will break the current
 * {@link Scope} implementation. For instance, the {@code ok} field in this class is not statically defined.
 * </p>
 */
abstract class EntryPoint extends ContainerBlock {
  private static final long serialVersionUID = -6200729226128923293L

  private static def testPipeline = null
  static void setup(def pipeline) {
    testPipeline = pipeline
  }
  static void cleanup() {
    testPipeline = null
  }

  void start(Object pipeline, Closure<?> body) {
    if (testPipeline) this.jenkins = testPipeline
    else this.jenkins = pipeline

    this.env = this.jenkins.env
    this.ok = true
    this.stages = Collections.synchronizedMap([:])

    execute null, body
  }
}
