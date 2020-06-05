package com.lilly.cirrus.jenkinsdsl.core

class OpenshiftPodTemplate extends PodTemplate {

  @Override
  String getLabel() {
    if(!this.@label) this.@label = "oc-dsl-${UUID.randomUUID().toString()}"
    return this.@label
  }

  String getImagePullSecretYaml() {
    if (!this.@imagePullSecrets) return ''
    StringBuilder builder = new StringBuilder()
    String text = """
  imagePullSecrets:"""
    builder.append(text)

    this.@imagePullSecrets.each {
      String secret = """
    - name: ${it}"""
      builder.append(secret)
    }
    return builder.toString()
  }

  protected String getPodSpec() {
    return """
apiVersion: "v1"
kind: "Pod"
spec:
  containers: ${this.getContainersYaml()} ${this.getImagePullSecretYaml()}
  ${this.@serviceAccount ? 'serviceAccount: ' + this.serviceAccount : ''}
"""
  }
}
