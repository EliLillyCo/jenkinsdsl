package com.lilly.cirrus.jenkinsdsl.core


import static com.lilly.cirrus.jenkinsdsl.core.DelegateFirstRunner.run

abstract class PodTemplate implements Serializable {
  private static final long serialVersionUID = 8043360053944778867L

  static final String DEFAULT_NAMESPACE = 'cje-slaves-freestyle-dmz'
  static final String DEFAULT_SERVICE_ACCOUNT = 'jenkins-slave'
  static final boolean CONTAINER_TTY_ENABLED = true
  static final String CONTAINER_COMMAND = "cat"

  protected String label
  protected String nameSpace
  protected String serviceAccount
  protected List<String> imagePullSecrets = []
  protected List<ArtifactoryImage> containers = []
  protected Map args

  String getNameSpace() {
    if (!this.@nameSpace) this.@nameSpace = DEFAULT_NAMESPACE
    return this.@nameSpace
  }

  String getServiceAccount() {
    if (!this.@serviceAccount) this.@serviceAccount = DEFAULT_SERVICE_ACCOUNT
    return this.@serviceAccount
  }

  List<String> getImagePullSecrets() {
    return this.@imagePullSecrets
  }

  void addImagePullSecret(String secret) {
    this.@imagePullSecrets.add(secret)
  }

  List<ArtifactoryImage> getContainers() {
    this.@containers
  }

  void addContainer(ArtifactoryImage container) {
    this.@containers.add(container)
  }

  String getContainersYaml() {
    StringBuilder stringBuilder = new StringBuilder()
    this.@containers.each { container ->
      stringBuilder.append(getContainerYaml(container))
    }
    return stringBuilder.toString()
  }

  String getContainerYaml(ArtifactoryImage image) {
    String spec = """
  - name: ${image.nameWithTag}
    image: ${image.image}
    resources:
      limits:
        memory: 4Gi
    command:
    - ${PodTemplate.CONTAINER_COMMAND}
    tty: ${PodTemplate.CONTAINER_TTY_ENABLED}"""
    return spec
  }

  abstract String getLabel()
  abstract protected String getPodSpec()

  def execute(def jenkins, Closure<?> closure) {
    if (!this.containers) throw new CirrusPipelineException('The pod is missing containers to run')

    def label = this.getLabel()
    run(jenkins) {
      podTemplate(label: label, namespace: this.nameSpace, yaml: this.getPodSpec()) {
        node(label) {
          run jenkins, closure
        }
      }
    }
  }
}
