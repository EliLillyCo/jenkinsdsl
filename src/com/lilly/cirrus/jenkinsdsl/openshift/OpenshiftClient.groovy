package com.lilly.cirrus.jenkinsdsl.openshift

import com.lilly.cirrus.jenkinsdsl.container.Label
import com.lilly.cirrus.jenkinsdsl.core.ArtifactoryImage
import com.lilly.cirrus.jenkinsdsl.core.CirrusPipelineException
import com.lilly.cirrus.jenkinsdsl.core.OpenshiftPodTemplate
import com.lilly.cirrus.jenkinsdsl.core.PodTemplate
import com.lilly.cirrus.jenkinsdsl.utility.Exceptions
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

import static com.lilly.cirrus.jenkinsdsl.core.DelegateFirstRunner.run

class OpenshiftClient implements Serializable {
  private static final long serialVersionUID = 2745681873173850600L
  static final String DEFAULT_REGISTRY_CREDENTIALS_ID = 'default-docker-credentials'
  static final String BUILD_CONFIG_TEMP_FILE = 'bc.json'

  def jenkins
  protected String jobName
  protected def registrySecrets = [:]
  protected def buildConfigs = [:]
  protected def namespacesWithCredentials = []
  String containerLabel

  String normalize(String text) {
    text = text.replaceAll("[^a-zA-Z0-9]", "").toLowerCase()
    int textLen = text.length()
    return text.substring(0, textLen > 45 ? 45 : textLen)
  }

  String getJobName() {
    if(this.@jobName) return this.@jobName

    this.@jobName = normalize(this.jenkins.env.JOB_NAME)
    return this.@jobName
  }

  String getSecretName(String registry) {
    getOrCreateName(registry, registrySecrets, "sc")
  }

  String getBuildConfigName(String image) {
    getOrCreateName(image, buildConfigs, "bc")
  }

  protected String getOrCreateName(String key, Map map, String suffix) {
    if (map.containsKey(key))
      return map[key]

    int id = map.size() + 1
    String name = "dsl.${getJobName()}${id}.${suffix}"
    map[key] = name
    return name
  }

  void runInPod(String namespace, String containerName='ocp-base-dsl', Closure<?> closure) {
    ArtifactoryImage image = new ArtifactoryImage(
      nameWithTag: containerName,
      image: 'openshift3/jenkins-slave-base-rhel7:v3.11'
    )
    PodTemplate pod = new OpenshiftPodTemplate(
      nameSpace: namespace,
      serviceAccount: 'jenkins-slave'
    )
    pod.addContainer(image)

    pod.execute(this.jenkins) {
      container(containerName) {
        closure()
      }
    }
  }

  void configureCredentials(Map namespaceToRegistryToCredentialId) {
    namespaceToRegistryToCredentialId.each { namespace, registryToCredentialId ->
      this.namespacesWithCredentials << namespace
      runInPod(namespace) {
        registryToCredentialId.each { registry, credentialId ->
          String secretName = this.getSecretName(registry)

          run(jenkins) {
            try {
            withCredentials([usernamePassword(credentialsId: "${credentialId}",
              usernameVariable: "USER", passwordVariable: "PASSWORD")]) {
              openshift.withCluster {
                def secret = openshift.selector("secret", secretName)
                if (secret.exists()) {
                  echo "Deleting existing Openshift secret [${secretName}] ..."
                  secret.delete()
                }

                echo "Creating new Openshift secrets [namespace: ${namespace}, registry: ${registry}, secret: ${secretName}] ..."
                openshift.secrets([
                  "new-dockercfg",
                  "${secretName}",
                  "--docker-email=${USER}",
                  "--docker-username=${USER}",
                  "--docker-password=${PASSWORD}",
                  "--docker-server=${registry}"
                ])
                openshift.raw([
                  "label",
                  "secret/${secretName}",
                  "jobName=${secretName}"
                ])
              }
            }
            } catch (e) {
                echo "Failed to Create new OpenShift secrets [namespace: ${namespace}, registry: ${registry}, secret: ${secretName}] ..."
                throw new Exception("Something went wrong during OpenShift configureCredentials!")
            }

          }
        }
      }
    }
  }

  String getBuildConfig(ArtifactoryImage containerImage, String dockerFile) {
    String image = containerImage.image
    String buildConfigName = this.getBuildConfigName(image)
    String registrySecretName = this.getSecretName(containerImage.registry)

    return """{
      "apiVersion": "v1",
      "kind": "BuildConfig",
      "metadata": {
        "name": "${buildConfigName}",
        "labels": {
          "jobName": "${buildConfigName}"
        }
      },
      "spec": {
        "output": {
          "to": {
            "kind": "DockerImage",
            "name": "${image}"
          },
          "pushSecret": {
            "name": "${registrySecretName}"
          }
        },
        "resources": {},
        "source": {
          "type": "Dockerfile",
          "dockerfile": ${dockerFile}
        },
        "strategy": {
          "type": "Docker",
          "dockerStrategy": {
            "pullSecret": {
              "name": "${registrySecretName}"
            }
          }
        }
      }
    }"""
  }

  void deleteSecrets() {
    this.namespacesWithCredentials.each { namespace ->
      runInPod(namespace) {
        run(jenkins) {
          openshift.withCluster {
            this.registrySecrets.values().each {
              def secret = openshift.selector("secret", it)
              if (secret.exists()) {
                echo "Deleting secret [${it}] ..."
                secret.delete()
              }
            }

            this.buildConfigs.values().each {
              def buildConfig = openshift.selector("buildconfig", it)
              if (buildConfig.exists()) {
                echo "Deleting build config [${it}] ..."
                buildConfig.delete()
              }
            }
          }
        }
      }
    }
  }


  void createDockerImage(Map buildArgs, ArtifactoryImage containerImage, String dockerFile, String namespace) {
    runInPod(namespace) {
      run(jenkins) {
        openshift.withCluster {
          String buildConfigName = this.getBuildConfigName(containerImage.image)

          def buildConfig = openshift.selector("buildconfig", buildConfigName)
          if (buildConfig.exists()) {
            echo "Deleting existing build config [${buildConfigName}] ..."
            buildConfig.delete()
          }

          String bcText = this.getBuildConfig(containerImage, dockerFile)
          writeFile file: "bc.json", text: "${bcText}"
          openshift.create("-f", "bc.json")

          String credentialsId = OpenshiftClient.DEFAULT_REGISTRY_CREDENTIALS_ID
          try {
            withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              echo "Build config: $buildConfigName"
              String customBuildArgs = this.addBuildArgs(buildArgs)
              echo "custom build args: ${customBuildArgs}"
              echo "starting build with these commands $buildConfigName, '--follow', '--wait=true', $customBuildArgs, \"--build-arg=USERNAME=${USERNAME}\", \"--build-arg=PASSWORD=${PASSWORD}\""
              if (!dockerFile.isEmpty()) {
                echo "Dockerfile exists!"
                if(customBuildArgs.isEmpty()) {
                  openshift.startBuild(buildConfigName, '--follow', '--wait=true', "--build-arg=USERNAME=${USERNAME}", "--build-arg=PASSWORD=${PASSWORD}")
                }else {
                  openshift.startBuild(buildConfigName, '--follow', '--wait=true', customBuildArgs, "--build-arg=USERNAME=${USERNAME}", "--build-arg=PASSWORD=${PASSWORD}")
                }
                openshift.startBuild(buildConfigName, '--follow', '--wait=true')
              }
            }
          } catch (e) {
            // The exception is a hudson.AbortException with details
            // about the failure.
            echo "Error encountered bulding Docker image : ${bcText}"
            throw new Exception("Something went wrong during OpenShift Docker build!")
          }
        }
      }
    }
  }

  String addBuildArgs(Map buildArgs) {
    String prefix = ''
    StringBuilder stringBuilder = new StringBuilder()
    buildArgs.each {
      key,value ->
        if (key == 'USERNAME' || key == 'PASSWORD') {
          throw new CirrusPipelineException("Build args cannot contain USERNAME and PASSWORD values ")
        } else {
          stringBuilder.append(prefix).append("--build-arg=${key}=${value}")
          prefix = ', '
        }
    }
    return stringBuilder.toString()
  }

  def environmentVariablesForBuild(def credentials, def envVars) {
    if (credentials) {
      for (int i = 0; i < credentials.asType(List).size(); i++) {
        def credential = credentials[i]
        try {
          jenkins.withCredentials([
            jenkins.string(
              credentialsId: credential,
              variable: "BUILD_CREDENTIAL"
            )
          ]) {
            withCluster {
              envVars[credential] = jenkins["BUILD_CREDENTIAL"]
              jenkins.echo "Reading ${credential} from credentials: ${jenkins["BUILD_CREDENTIAL"]}"
            }
          }
        } catch (CredentialNotFoundException exception) {
          jenkins.echo "Jenkins Credential not found for ${credential}."
        }
      }
    }
    envVars
  }

  void rollout(ReleaseEnvironment environment, String projectName, String image, String imageStreamName, String deployConfigName, String serviceAccountCredential, String nonProdUrl) {
    jenkins.withCredentials([
      jenkins.string(
        credentialsId: serviceAccountCredential,
        variable: "OS_PROJECT_SERVICE_ACCOUNT"
      )
    ]) {
      Closure openshiftClosure = {
        jenkins.openshift.withProject(projectName) {
          jenkins.openshift.withCredentials(jenkins.OS_PROJECT_SERVICE_ACCOUNT) {
            jenkins.openshift.raw("import-image", "--confirm=true", imageStreamName, "--from=${image}")
            jenkins.openshift.raw("set", "image", "dc/${deployConfigName}", "${deployConfigName}=${imageStreamName}", "--source=imagestreamtag")
            jenkins.openshift.selector("dc", deployConfigName).rollout().latest()
          }
        }
      }
      if (environment == ReleaseEnvironment.Production) {
        //do not use cluster
        jenkins.openshift.withCluster() {
          jenkins.echo "Rolling out to prod cluster"
          openshiftClosure()
        }
      } else {
        jenkins.openshift.withCluster(nonProdUrl, jenkins.OS_PROJECT_SERVICE_ACCOUNT) {
          jenkins.echo "From project ${projectName}"
          openshiftClosure()
        }
      }
    }
  }

  String cleanValue(String value) {
    String possiblyShortenedValue = value.size() > Label.LENGTH_LIMIT ? value[-Label.LENGTH_LIMIT..-1] : value
    String noSlashesValue = possiblyShortenedValue.replaceAll('/', '.')
    Label kubernetesLabel = new Label(baseText: noSlashesValue)
    return kubernetesLabel.toString()
  }

  void deleteBuildConfigsByLabel(String labelName, String labelValue) {
    withCluster {
      def result = jenkins.openshift.delete('buildconfig', '-l', "${labelName}=${cleanValue(labelValue)}")
      printResultOutput(result, 'delete')
    }
  }

  void printResultOutput(result, label) {
    if (result && result.actions) {
      jenkins.echo "[${label}] > ${result.actions[0]?.cmd}"
      jenkins.echo "[${label}] ${result.actions[0]?.out.replaceAll('\n', '\n[' + label + '] ')}"
    }
  }

  void labelSecret(String name, String labelName, String labelValue) {
    withCluster {
      def result = jenkins.openshift.raw('label', "--overwrite", "secret/${name.toLowerCase()}", "${labelName}=${cleanValue(labelValue)}")
      printResultOutput(result, 'label')
    }
  }

  def generateBuildConfig(String json) {
    withCluster {
      jenkins.writeFile file: BUILD_CONFIG_TEMP_FILE, text: json
      def result = jenkins.openshift.create('-f', BUILD_CONFIG_TEMP_FILE)
      printResultOutput(result, 'create')
    }
  }

  void createDockerSecret(String name, String registry, String username, String password) {
    withCluster {
      def result = jenkins.openshift.secrets(
        SecretType.DOCKER_REGISTRY.typeIdentifier,
        name.toLowerCase(),
        "--docker-email=${username}",
        "--docker-username=${username}",
        "--docker-password=${password}",
        "--docker-server=${registry}"
      )
      printResultOutput(result, 'secrets')
    }
  }

  void build(String buildConfigName, String user='', String password='') {

    withCluster {
      try {
        if(jenkins.fileExists('Dockerfile')) {
          jenkins.openshift.startBuild(buildConfigName, '--follow', '--wait=true', "--build-arg=USERNAME=${user}", "--build-arg=PASSWORD=${password}")
        } else {
          jenkins.openshift.startBuild(buildConfigName, '--follow', '--wait=true')
        }
      } catch (Exception e) {
        jenkins.echo "Could not complete Openshift build for user User: ${user}"
        throw new Exception("Something went wrong during OpenShift build!")
      }
    }
  }

  void deleteSecretsByLabel(String labelName, String labelValue) {
    withCluster {
      def result = jenkins.openshift.delete('secret', '-l', "${labelName}=${cleanValue(labelValue)}")
      printResultOutput(result, 'delete')
    }
  }

  void createTokenSecret(SecretType type, String name, String token) {
    jenkins.echo "S2I SecretType: ${type.typeIdentifier}, Name: ${name}, Token: ${token}"

    withCluster {
      try {
        def result = jenkins.openshift.secrets(type.typeIdentifier, name.toLowerCase(), "--password=${token}")
        printResultOutput(result, 'secrets')
      }
      catch (Exception e) {
        jenkins.echo "Could not setup secret [type: ${type.typeIdentifier}, name: ${name}]"
        throw new Exception("Something went wrong during OpenShift createTokenSecret!")
      }
    }
  }

  // Remember this is different than openshift.withCluster as containerLabel cannot be null
  private def withCluster(Closure clusterClosure) {
    jenkins.openshift.withCluster() {
      jenkins.container(containerLabel) {
        clusterClosure()
      }
    }
  }

  private def withProject(String projectName, Closure projectClosure) {
    jenkins.openshift.withProject(projectName) {
      projectClosure()
    }
  }

}
