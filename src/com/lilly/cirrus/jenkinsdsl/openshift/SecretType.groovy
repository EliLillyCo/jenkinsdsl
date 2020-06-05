package com.lilly.cirrus.jenkinsdsl.openshift

enum SecretType {
  BASIC_AUTHENTICATION('new-basicauth'),
  DOCKER_REGISTRY('new-dockercfg')
  
  String typeIdentifier
  
  SecretType(String typeIdentifier) {
    this.@typeIdentifier = typeIdentifier
  }
}
