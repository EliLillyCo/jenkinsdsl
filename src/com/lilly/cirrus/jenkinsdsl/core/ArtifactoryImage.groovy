package com.lilly.cirrus.jenkinsdsl.core

class ArtifactoryImage implements Serializable {
  private static final long serialVersionUID = -7663536829863179046L
  static String ARTIFACTORY_URL = "https://elilillyco.jfrog.io/elilillyco"

  protected String registry
  protected String name
  protected String tag = "latest"
  protected String nameWithTag
  protected String image

  protected String artifactoryServer
  protected String artifactoryImageRepo

  String getRegistry() {
    return this.@registry
  }

  String getName() {
    return this.@name
  }

  String getTag() {
    return this.@tag
  }

  String getArtifactoryServer() {
    if (this.@artifactoryServer) return this.@artifactoryServer
    return ARTIFACTORY_URL
  }

  String getArtifactoryImageRepo() {
    if (this.@artifactoryImageRepo) return this.@artifactoryImageRepo

    String hostName = this.@registry.tokenize('.').first()
    this.@artifactoryImageRepo = hostName.replace('elilillyco-', '')
    return this.@artifactoryImageRepo
  }

  String getImage() {
    return this.@image ?: "${registry}/${name}:${tag}"
  }

  String getNameWithTag() {
    if (this.@nameWithTag) return this.@nameWithTag
    String nameWithTag = "${name}-${tag}"
    return nameWithTag.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase()
  }

  String getUrl() {
    "${this.getArtifactoryServer()}/list/${this.getArtifactoryImageRepo()}/${this.getName()}/${this.getTag()}"
  }

  @Override
  boolean equals(o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    ArtifactoryImage that = (ArtifactoryImage) o

    if (name != that.name) return false
    if (registry != that.registry) return false
    if (tag != that.tag) return false

    return true
  }

  @Override
  int hashCode() {
    int result
    result = (registry != null ? registry.hashCode() : 0)
    result = 31 * result + (name != null ? name.hashCode() : 0)
    result = 31 * result + (tag != null ? tag.hashCode() : 0)
    return result
  }

  @Override
  String toString() {
    return this.getImage()
  }

  static ArtifactoryImage fromString(String image) {
    if (!image) throw new CirrusPipelineException('You need to provide a container image name.')
    List<String> parts = image.tokenize('/')
    if (parts.size() < 2) throw new CirrusPipelineException("Invalid docker image provided")

    String registry = parts.first()
    String rest = image.substring(registry.length() + 1)

    parts = rest.tokenize(":")
    String name = parts.first()

    String tag = "latest"
    if (parts.size() > 1)
      tag = rest.substring(name.length() + 1)

    return new ArtifactoryImage(registry: registry, name: name, tag: tag)
  }
}
