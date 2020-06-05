package com.lilly.cirrus.jenkinsdsl.core

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.mock.block.*
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class BlockSpec extends CirrusDSLSpec {

  def "when a block is created, its name and env are initialized by default"() {
    when: "a new block is created"
    BlockSimpleMock block = new BlockSimpleMock()

    then: "its name is initialized to runtime class name of the block"
    block.name == block.getClass().simpleName
  }

  def "when a DSL block is executed, it should expand the scope of parent and run only when ok to do so"() {
    given: "a DSL block and its parent with ok set to false"
    BlockSimpleMock block = new BlockSimpleMock()
    BlockSimpleMock parent = new BlockSimpleMock()
    parent.ok = false

    when: "execute is called on the block with a closure"
    block.execute(parent, {closureRan = true})

    then: "it should expand the scope"
    block.parent != null

    and: "it should terminate without executing the closure"
    !block.closureRan
  }


  def "when executing a DSL block, it should expand the scope of parent, run when ok to do so, and reduce the scope"() {
    given: "a DSL block and its parent with ok set to true"
    BlockScopeMock parent = new BlockScopeMock()
    parent.ok = true
    BlockScopeMock block = new BlockScopeMock()

    when: "execute is called on the block with a closure"
    block.execute(parent, {closureRan = true})

    then: "it should expand the scope"
    block.scopeExpanded

    then: "it should run the closure"
    block.closureRan

    then: "it should reduce the scope"
    block.scopeReduced
  }

  def "when invoking block, it should not execute when it is not ok to do so"() {
    given: "a DSL block and with ok set to false"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = false

    when: "block is called on the block with a closure"
    block.runBlock(null, {closureRan = true})

    then: "it should not run the closure"
    !block.closureRan
  }

  def "when invoking block(), it should create a new block and set name, and execute the body, when it is ok to do so"() {
    given: "a DSL block and with ok set to true"
    BlockCreateMock block = new BlockCreateMock()
    block.ok = true

    when: "block() is called on the block with a closure"
    block.runBlock("CI", {closureRan = true})

    then: "it should create a new block with the supplied name"
    block.createdBlock.name == "CI"

    and: "it should execute the closure on the newly created block"
    block.createdBlock.closureRan
  }


  def "when invoking block(), it should create a new block and only set name the name if it is supplied"() {
    given: "a DSL block and with ok set to true"
    BlockCreateMock block = new BlockCreateMock()
    block.ok = true

    when: "block() is called on the block with non name and a closure "
    block.runBlock(null, {closureRan = true})

    then: "it should create a new block with the default name"
    block.createdBlock.name == block.createdBlock.getClass().simpleName

    and: "it should execute the closure on the newly created block"
    block.createdBlock.closureRan
  }

  def "a stage should not execute when it is not ok to so"() {
    given: "a DSL block and with ok set to false and an stage object"
    BlockCreateMock block = new BlockCreateMock()
    block.ok = false
    Stage stage = Mock(Stage)


    when: "execute() is called on the block with the given stage"
    block.execute(stage)

    then: "it should not execute the stage"
    0 * stage.execute(block)
  }

  def "a stage should only execute when it is ok to do so"() {
    given: "a DSL block and with ok set to false and an stage object"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true
    Stage stage = Mock(Stage)


    when: "execute() is called on the block with the given stage"
    block.execute(stage)

    then: "it should not execute the stage"
    1 * stage.execute(block)
  }


  def "task should not execute when it is not ok to do so"() {
    given: "a DSL block with ok set to false"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = false


    when: "task() is called on the block with a parallel closure"
    block.runTask("task", {})

    then: "it should not execute the task body"
    !block.@parallelTasks
  }

  def "task should execute when it is ok to do so"() {
    given: "a DSL block with ok set to true and a mock closure"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true
    Closure<?> closure = {}

    when: "task() is called on the block with a parallel closure"
    block.runTask("task", closure)

    then: "it should set the task correctly"
    block.@parallelTasks.task == closure
  }


  def "setupParallelBlocks() should setup nothing for empty tasks"() {
    given: "a DSL block with ok set to true and no parallel tasks"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true

    when: "setupParallelBlocks() is called on the block"
    block.setupParallelBlocks()

    then: "it should set up three parallel blocks"
    !block.@parallelBlocks
  }

  def "tearDownParallelBlocks() should reduce scope of all parallel blocks"() {
    given: "a DSL block with ok set to true and some parallel blocks"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true

    block.parallelBlocks.first = block.createBlock()
    block.parallelBlocks.first.expandScope(block)

    block.parallelBlocks.second = block.createBlock()
    block.parallelBlocks.second.expandScope(block)

    when: "tearDownParallelBlocks() is called on the block"
    block.tearDownParallelBlocks()

    then: "it should reduce the scope of all parallel blocks"
    !block.parallelBlocks.second.parent
    !block.parallelBlocks.second.parent
  }

  def "tearDownParallelBlocks() should teardown nothing for empty blocks"() {
    given: "a DSL block with ok set to true and no parallel tasks"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true

    when: "tearDownParallelBlocks() is called on the block"
    block.tearDownParallelBlocks()

    then: "it should set up three parallel blocks"
    !block.@parallelBlocks
  }

  def "executeParallelBlock should not execute if it is not ok to do so"() {
    given: "a parent and a child DSL blocks with parent not ok to execute"
    BlockParallelMock parent = new BlockParallelMock()
    parent.ok = false
    BlockParallelMock block = new BlockParallelMock()
    block.parallelTasks.first = {}

    when: "executeParallelBlock() is called on the block"
    block.executeParallelBlocks(parent)

    then: "it should expand the scope to include the parent"
    block.parent == parent

    then: "parallel block setup should not be executed"
    !block.setupParallelBlocksExecuted
  }

  def "executeParallelBlock should not execute if there are no parallel task to run"() {
    given: "a parent and a child DSL blocks with parent not ok to execute"
    BlockParallelMock parent = new BlockParallelMock()
    parent.ok = true
    BlockParallelMock block = new BlockParallelMock()

    when: "executeParallelBlock() is called on the block"
    block.executeParallelBlocks(parent)

    then: "it should expand the scope to include the parent"
    block.parent == parent

    then: "parallel block setup should not be executed"
    !block.setupParallelBlocksExecuted
  }

  def "executeParallelBlock should execute if it is ok to do so with default failFast option"() {
    given: "a parent and a child DSL blocks with parent with ok set to true with some parallel tasks"
    BlockParallelMock parent = new BlockParallelMock()
    parent.ok = true
    BlockParallelMock block = new BlockParallelMock()
    block.parallelTasks.first = {first = true}
    block.parallelTasks.second = {second = true}
    JenkinsSim pipeline = createJenkinsSim()
    parent.jenkins = pipeline

    when: "executeParallelBlock() is called on the block"
    block.executeParallelBlocks(parent)

    then: "it should expand the scope to include the parent"
    block.expandScopeExecuted

    then: "it should setup the parallel blocks"
    block.setupParallelBlocksExecuted

    then: "it should run parallel task with default failFast option"
    pipeline.trace SimFactory.parallel(failFast: true, block.parallelTasks)

    then: "it should teardown the parallel blocks"
    block.tearDownParallelBlocksExecuted

    then: "it should reduce the scope"
    block.reduceScopeExecuted
  }

  def "executeParallelBlock should execute if it is ok to do so with the set failFast options"() {
    given: "a parent and a child DSL blocks with parent with ok set to true with some parallel tasks"
    BlockParallelMock parent = new BlockParallelMock()
    parent.ok = true
    BlockParallelMock block = new BlockParallelMock()
    block.parallelTasks.first = {first = true}
    block.parallelTasks.second = {second = true}
    block.failFast = false
    JenkinsSim pipeline = createJenkinsSim()
    parent.jenkins = pipeline

    when: "executeParallelBlock() is called on the block"
    block.executeParallelBlocks(parent)

    then: "it should expand the scope to include the parent"
    block.expandScopeExecuted

    then: "it should setup the parallel blocks"
    block.setupParallelBlocksExecuted

    then: "it should run parallel task with the set failFast option"
    pipeline.trace SimFactory.parallel(failFast: false, block.parallelTasks)

    then: "it should teardown the parallel blocks"
    block.tearDownParallelBlocksExecuted

    then: "it should reduce the scope"
    block.reduceScopeExecuted
  }


  def "a parallel block should not execute when it is not ok to so"() {
    given: "a DSL block with ok set to false"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = false


    when: "parallel() is called on the block with a parallel closure"
    block.runParallel({parallelExecuted = true})

    then: "it should not execute the parallel body"
    !block.parallelExecuted
  }

  def "a parallel block should execute when it is ok to so"() {
    given: "a DSL block with ok set to false"
    BlockSimpleMock block = new BlockSimpleMock()
    block.ok = true
    block.parallelExecuted = false

    when: "parallel() is called on the block with a parallel closure"
    block.runParallel({
      nearest("parallelExecuted").parallelExecuted = true
    })

    then: "it should execute the parallel body"
    block.parallelExecuted
  }

  def "a parallel block should get a default name when none is provided"() {
    given: "a DSL block with ok set to false"
    BlockNameCheckMock block = new BlockNameCheckMock()
    block.ok = true
    block.parallelExecuted = false

    when: "parallel() is called on the block without a name"
    block.runParallel({
      nearest("parallelExecuted").parallelExecuted = true
    })

    then: "it should get a default name"
    block.createdBlock.name != null

    then: "it should execute the parallel body"
    block.parallelExecuted
  }


  def "a parallel block should get a name when one is provided"() {
    given: "a DSL block with ok set to false"
    BlockNameCheckMock block = new BlockNameCheckMock()
    block.ok = true
    block.parallelExecuted = false

    when: "parallel() is called on the block without a name"
    block.runParallel("Parallel", {
      nearest("parallelExecuted").parallelExecuted = true
    })

    then: "it should get a default name"
    block.createdBlock.name == "Parallel"

    then: "it should execute the parallel body"
    block.parallelExecuted
  }

  def "a parallel block should setup parallel blocks and execute the parallel tasks"() {
    given: "a DSL block with ok set to false"
    BlockNameCheckMock block = new BlockNameCheckMock()
    block.ok = true
    JenkinsSim pipeline = createJenkinsSim()
    block.jenkins = pipeline

    when: "parallel() is called on the block with three parallel tasks and failFast option set to false"
    block.runParallel({
      failFast = false
      runTask("First") {first = true}
      runTask("Second") {second = true}
      runTask("Third") {third = true}
    })

    then: "it should have setup three parallel tasks"
    block.createdBlock.parallelTasks.size() == 4

    and: "it should have setup three parallel blocks"
    block.createdBlock.parallelBlocks.size() == 3

    and: "it should execute the parallel tasks"
    pipeline.trace SimFactory.parallel(failFast: true, First: {}, Second: {}, Third: {})
  }
}
