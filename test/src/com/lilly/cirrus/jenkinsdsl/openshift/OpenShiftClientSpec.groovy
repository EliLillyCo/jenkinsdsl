package com.lilly.cirrus.jenkinsdsl.openshift

import com.lilly.cirrus.jenkinsdsl.core.ArtifactoryImage
import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.core.CirrusPipelineException
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

class OpenShiftClientSpec extends CirrusDSLSpec {
  String getTestString(int length) {
    StringBuilder builder = new StringBuilder()
    for (int i = 0; i < length; ++i) {
      builder.append('A')
    }
    return builder.toString()
  }


  def "openshift client should return a jobname that can be used in openshift for creating secrets"() {
    given: "a mock environment and an openshift client"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    when: "a job name is retrieved"
    String jobName = openShiftClient.jobName

    then: "the job name returned will be 45 character long, all small"
    jobName.length() == 45
    jobName == this.getTestString(45).toLowerCase()
  }

  def "openshift client  should return a jobname that can be used in openshift for creating secrets even for long job names"() {
    given: "a mock environment and an openshift client"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(46)])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    when: "a job name is retrieved"
    String jobName = openShiftClient.jobName

    then: "the job name returned will be 45 character long, all small"
    jobName.length() == 45
    jobName == this.getTestString(45).toLowerCase()

    and: "the same job name is cached"
    openShiftClient.jobName == this.getTestString(45).toLowerCase()
  }

  def "getting a secret name creates a new secret name if one does not exist"() {
    given: "a mock environment and an openshift client"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)


    when: "a secret name for registry is retrieved"
    String secretName = openShiftClient.getSecretName("registry")

    then: "a secret name is created if one does not exist"
    secretName != null

    and: "the same secret name is return when asked for the same registry"
    openShiftClient.getSecretName("registry").is(secretName)
  }

  def "runInPod runs the given closure in openshift pod"() {
    given: "an openshift client configured with a registry credential id"
    String podNamespace = "cje-slaves-freestyle-dmz"
    String containerName = "my-container"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])

    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)
    openShiftClient.configureCredentials("${podNamespace}": ["registry": "cred-id"])
    boolean closureExecuted = false

    when: "A closure is run in a prod"
    openShiftClient.runInPod(podNamespace, containerName, { closureExecuted = true })

    then: "pipeline executes needed pipeline actions"
    pipeline.anchor(SimFactory.node({ label -> label.startsWith("oc-dsl") }))
      .next(SimFactory.container(containerName))

    and: "the closure gets executed"
    closureExecuted
  }

  def "creating a docker secret configures the pipeline with the secret"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding("cred-id", [USER: "user", PASSWORD: "password"])

    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    when: "docker secret is created"
    openShiftClient.configureCredentials("cje-slaves-freestyle-dmz": ["registry": "cred-id"])
    String secretName = openShiftClient.getSecretName("registry")

    then: "pipeline executes needed pipeline actions"
    pipeline.anchor(SimFactory.node({ label -> label.startsWith("oc-dsl") }))
      .next(SimFactory.container("ocp-base-dsl"))
      .next(SimFactory.secrets([
        "new-dockercfg",
        "${secretName}",
        "--docker-email=user",
        "--docker-username=user",
        "--docker-password=password",
        "--docker-server=registry"
      ]))
      .next(SimFactory.raw([
        "label",
        "secret/${secretName}",
        "jobName=${secretName}"
      ]))
  }

  def "createDockerImage should run the openshift startBuild command to build a docker image with custom build args"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding(OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID, [USERNAME: "user", PASSWORD: "password"])

    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    and: 'a container image, docker file, and namespace to use to build a new image'
    ArtifactoryImage image = ArtifactoryImage.fromString("elilillyco-lilly-docker.jfrog.io/python:3.7.9")
    String dockerFile = "A Mock Dockerfile"
    String namespace = "a-mock-namespace"

    Map buildArgs = [GITHUB_TOKEN: 'github-secret-token']

    when: "docker image is created"
    openShiftClient.createDockerImage(buildArgs, image, dockerFile, namespace)

    then: "pipeline executes needed pipeline actions"
    pipeline >> SimFactory.node({ label -> label.startsWith("oc-dsl") }) +
      SimFactory.container("ocp-base-dsl") +
      SimFactory.withCluster() +
      SimFactory.selector(['buildconfig', _]) +
      SimFactory.exists() +
      SimFactory.delete() +
      SimFactory.writeFile(file: "bc.json", text: _) +
      SimFactory.create(["-f", "bc.json"]) +
      SimFactory.startBuild([_, '--follow', '--wait=true', "--build-arg=GITHUB_TOKEN=github-secret-token",
                                                                   "--build-arg=USERNAME=user", "--build-arg=PASSWORD=password"])
  }

  def "deleteSecrets should cleanup any docker secrets and build configs by calling appropriate openshift commands and no build args"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding(OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID, [
      USERNAME: "user", PASSWORD: "password"
    ])

    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    and: 'a container image, docker file, and namespace to use to build a new image'
    ArtifactoryImage image = ArtifactoryImage.fromString("elilillyco-lilly-docker.jfrog.io/python:3.7.9")
    String dockerFile = "A Mock Dockerfile"
    String namespace = "cje-slaves-freestyle-dmz"

    when: "the delecteSecrets is called after creating docker secrets and building a new docker image"
    openShiftClient.configureCredentials("${namespace}": ["registry": OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID])
    openShiftClient.createDockerImage([:], image, dockerFile, namespace)
    openShiftClient.deleteSecrets()

    then: "pipeline executes necessary pipeline actions to delete the docker secrets and build configs"
    pipeline >> SimFactory.secrets(_) +
      SimFactory.startBuild([_, '--follow', '--wait=true', "--build-arg=USERNAME=user", "--build-arg=PASSWORD=password"]) +
      SimFactory.container("ocp-base-dsl") +
      SimFactory.withCluster() +
      SimFactory.selector(['secret', _]) +
      SimFactory.exists() +
      SimFactory.delete() +
      SimFactory.selector(['buildconfig', _]) +
      SimFactory.exists() +
      SimFactory.delete()
  }

  def "environment variables for build will set credentials"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding(OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID, [
      USERNAME: "user", PASSWORD: "password",
      USER: "user", PASSWORD: "password"
    ])
    def appConfig = [
      envCredentials: ['cred1', 'cred2'],
      envVariables: [:]
    ]
    pipeline.withCredentialBinding('cred1', [
      'BUILD_CREDENTIAL': 'testing1'
    ])
    pipeline.withCredentialBinding('cred2', [
      'BUILD_CREDENTIAL': 'testing2'
    ])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    when: "method called"
    def envs = openShiftClient.environmentVariablesForBuild(appConfig.envCredentials, appConfig.envVariables)

    then: 'envs should have values'
    envs.cred1 == 'testing1'
    envs.cred2 == 'testing2'
  }

  def "environment variables call will echo cred not found for all not found creds"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    pipeline.withCredentialBinding(OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID, [
      USERNAME: "user", PASSWORD: "password",
      USER: "user", PASSWORD: "password"
    ])
    def appConfig = [
      envCredentials: ['cred1', 'cred2'],
      envVariables: [:]
    ]
    pipeline.withCredentialBinding('cred1', [
      'BUILD_CREDENTIAL': 'testing1'
    ])
    pipeline.withCredentialBinding('cred2', [
      'BUILD_CREDENTIAL': 'testing2'
    ])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    pipeline.when(SimFactory.echo({it.contains('Reading')})).then {
      throw new CredentialNotFoundException('testing')
    }

    when: "method called"
    openShiftClient.environmentVariablesForBuild(appConfig.envCredentials, appConfig.envVariables)

    then: 'should break'
    pipeline >> SimFactory.echo({it.contains('Jenkins Credential not found for cred1')}) +
      SimFactory.echo({it.contains('Jenkins Credential not found for cred2')})

  }

  def "build will not call with build arg if no docker file"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    pipeline.when(SimFactory.fileExists('Dockerfile')).then {
      false
    }

    when: "build is called"
    openShiftClient.build('fake_name')

    then: "the correct start build is called"
    pipeline >> SimFactory.startBuild(['fake_name', '--follow', '--wait=true'])
  }

  def "create token secret will catch and throw an error on an error"() {
    given: "an openshift client configured with a registry credential id"
    JenkinsSim pipeline = createJenkinsSim()
    pipeline.withEnv([JOB_NAME: this.getTestString(45)])
    OpenshiftClient openShiftClient = new OpenshiftClient(jenkins: pipeline)

    pipeline.when(SimFactory.secrets([_, _, "--password=token"])).then {
      throw new Exception('testing')
    }

    when: "createTokenSecret is called"
    openShiftClient.createTokenSecret(SecretType.DOCKER_REGISTRY, 'name', 'token')

    then: "the correct jenkins calls are called"
    pipeline >> SimFactory.echo("Could not setup secret [type: new-dockercfg, name: name]") +
      SimFactory.echo({it.contains('testing')})

    and: 'throws'
    thrown(Exception)
  }

  def "openShift addBuildArgs test when buildArgs map contains username"() {
    given: "an openShift client"
    JenkinsSim jenkinsSim = createJenkinsSim()
    OpenshiftClient osc = new OpenshiftClient(jenkins: jenkinsSim)

    and: "custom build args map"
    Map customBuildArgs = [USERNAME: "USERNAME", GITHUB_TOKEN: "token"]

    when: "openShift client addBuildArgs is called"
    osc.addBuildArgs(customBuildArgs)

    then: "should throw an expected exception"
    thrown(CirrusPipelineException)
  }

  def "openShift addBuildArgs test when buildArgs map contains password"() {
    given: "an openShift client"
    JenkinsSim jenkinsSim = createJenkinsSim()
    OpenshiftClient osc = new OpenshiftClient(jenkins: jenkinsSim)

    and: "custom build args map"
    Map customBuildArgs = [PASSWORD: "USERNAME", GITHUB_TOKEN: "token"]

    when: "openShift client addBuildArgs is called"
    osc.addBuildArgs(customBuildArgs)

    then: "should throw an expected exception"
    thrown(CirrusPipelineException)
  }

  def "openShift addBuildArgs test when valid buildArgs map is passed"() {
    given: "an openShift client"
    JenkinsSim jenkinsSim = createJenkinsSim()
    OpenshiftClient osc = new OpenshiftClient(jenkins: jenkinsSim)

    and: "custom build args map"
    Map customBuildArgs = [GITHUB_TOKEN: 'github-secret-token', JIRA_TOKEN: 'jira-secret-token']

    when: "openShift client addBuildArgs is called"
    String buildArgsCommand = osc.addBuildArgs(customBuildArgs)

    then: "should not throw an exception"
    notThrown(CirrusPipelineException)

    then: "should return expected buildArgs command"
    buildArgsCommand == "--build-arg=GITHUB_TOKEN=github-secret-token, --build-arg=JIRA_TOKEN=jira-secret-token"
  }
}
