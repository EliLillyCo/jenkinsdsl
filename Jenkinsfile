@Library('cirr-shared-libraries@v13.1.0-beta') _

javaPipeline {
  POD_NAMESPACE = 'cje-slaves-artifactory-dmz'
  OPENSHIFT_NAMESPACE = 'cje-slaves-openshift-dmz'

  dynamicDockerRegistry = "elilillyco-cirr-dynamic-docker-lc.jfrog.io"
  builderImage = "${dynamicDockerRegistry}/java:13.0.2.j9-adpt"

  registryToCredentialId = ["${dynamicDockerRegistry}": "default-docker-credentials"]
  configureCredentials "${OPENSHIFT_NAMESPACE}": registryToCredentialId,
    "${POD_NAMESPACE}": registryToCredentialId

  onFinish { deleteCredentials() }

  runPod(POD_NAMESPACE) {
    runSetup { abortPreviousBuilds() }

    runContainer(builderImage) {
      onFinish {
        postResultsToPullRequest()
        postToSlack()
        postToJira()
      }

      checkoutGit()
      readAppConfig()
      getGitHubEventType()

      checkBuildConfig()
      checkVersion()
      
      runParallel('Tests and Checks') {
        failFast = false	
        runTask("Java Build") { runBuild() }	
        runTask("Open Source Dependency Security Check") { runSnyk() }	
      }
      
      deployToArtifactory()
    }
  }
}
