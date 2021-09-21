package com.lilly.cirrus.jenkinsdsl.core


import com.lilly.cirrus.jenkinsdsl.utility.Exceptions
import com.lilly.cirrus.jenkinsdsl.openshift.OpenshiftClient
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

abstract class ContainerBlock extends Block {
  private static final long serialVersionUID = 1L

  protected Map<String, Closure> containerTasks = [:]
  protected Map<String, Block> containerBlocks = [:]
  protected Map<String, Map> containerArgs = [:]

  protected Closure<?> setupClosure
  protected Closure<?> tearDownClosure

  protected void runSetup(Closure<?> body) {
    this.setupClosure = body
  }

  protected void runTearDown(Closure<?> body) {
    this.tearDownClosure = body
  }

  protected void configureCredentials(Map namespaceToRegistryToCredentialId) {
    OpenshiftClient openShiftClient = this.openShiftClient

    if (!openShiftClient) {
      openShiftClient = new OpenshiftClient(jenkins: this.jenkins)
      root().openShiftClient = openShiftClient
    }
    withJenkins {
        echo "Adding comments in the preperation of timeout for the pipeline"
        sh "/bin/sleep 120"
        sh "ls /bin/sleep"
        this.jenkins.currentBuild.result = 'ABORTED'

    }

    openShiftClient.configureCredentials(namespaceToRegistryToCredentialId)
  }

  protected void deleteCredentials() {
    OpenshiftClient openShiftClient = this.openShiftClient

    if (!openShiftClient) return

    openShiftClient.deleteSecrets()
  }



  protected void runPod(String nameSpace = PodTemplate.DEFAULT_NAMESPACE, Map args = [:], Closure<?> body) {
    if (!this.ok) return

    ContainerBlock enclosingBlock = createBlock()
    if (nameSpace) enclosingBlock.name = nameSpace


    PodTemplate podTemplate = new OpenshiftPodTemplate(nameSpace: nameSpace, args: args)
    enclosingBlock.pod = podTemplate

    enclosingBlock.execute(this, body) // Sets up container when the runContainer() method gets called in the body
    enclosingBlock.executeContainerBlocks(this) // Executes the containers
  }


  protected void executeContainerBlocks(Block parent) {
    expandScope(parent)

    if (!this.ok) return

    setupContainerBlocks()

    withJenkins {
      def imagePullSecrets = this.openShiftClient?.registrySecrets?.values()?.asList()
      this.pod.imagePullSecrets = imagePullSecrets
      this.pod.execute(this.jenkins) {
        if (this.@setupClosure) DelegateFirstRunner.run this, this.@setupClosure

        Exception exception = null
        try {
          if (this.parallelContainers) this.runContainersParallely()
          else this.runContainersSerially()
        }
        catch (Exception e) {
          exception = e
        }

        if (this.@tearDownClosure) DelegateFirstRunner.run this, this.@tearDownClosure, exception
        if (exception) throw exception
      }
    }

    tearDownContainerBlocks()
    reduceScope()
  }

  protected void runContainersSerially() {
    this.@containerTasks.values().each { it() }
  }

  protected void runContainersParallely() {
    this.@containerTasks.failFast = (this.failFast == false? false : true)
    withJenkins { parallel this.@containerTasks }
  }

  protected void setupContainerBlocks() {
    Map<String, Closure> parallelContainerTasks = [:]

    this.@containerTasks.each { name, body ->
      Block block = this.createBlock()
      block.name = name
      this.@containerBlocks[name] = block

      Closure<?> containerTask = {
        block.containerName = name
        block.container = this.@containerArgs[name].container
        container(name) {
          Exception exception = null
          try {
            DelegateFirstRunner.run block, body
          }
          catch (Exception e) {
            exception = e
            this.setCurrentBuildStatus(exception)
          }

          if (block.@finishingClosure) DelegateFirstRunner.run block, block.@finishingClosure, exception
          if (exception) throw exception
        }
      }

      DelegateFirstRunner.setup this.jenkins, containerTask
      parallelContainerTasks[name] = containerTask

      block.expandScope(this)
    }

    this.@containerTasks = parallelContainerTasks
  }

  protected void setCurrentBuildStatus(Exception e) {
    if (e) Exceptions.printStackTrace(e)
    if (checkAborted(e)) {
      this.jenkins.currentBuild.result = 'ABORTED'
    }
    else {
      this.jenkins.currentBuild.result = 'FAILURE'
    }
  }

  protected boolean checkAborted(Throwable e) {
    if (e instanceof FlowInterruptedException) return true
    if (!e?.cause || e.cause.is(e)) return false
    return checkAborted(e.cause)
  }

  protected void tearDownContainerBlocks() {
    this.@containerBlocks.values().each {it.reduceScope()}
  }

  protected void runContainer(String image = null, Map args = [:], Closure<?> body) {
    if (!this.ok) return

    if (!image) image = args.image

    if (!this.getLocalProperty("pod"))
      throw new IllegalStateException("A container must run inside a pod block and must not be nested.")

    // enterpriseImage is defined in an image preparation stage
    if (!image) image = this.enterpriseImage

    ArtifactoryImage container = ArtifactoryImage.fromString(image)
    this.pod.addContainer(container)

    args.container = container
    this.@containerTasks[container.nameWithTag] = body
    this.@containerArgs[container.nameWithTag] = args
  }
}
