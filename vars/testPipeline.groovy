def call() {
  def dslPipeline = new freeFormPipeline()

  dslPipeline {
    nodeImage = "elilillyco-lilly-docker.jfrog.io/node:8.16.0"

    runPod {
      runContainer(nodeImage) {
        runStage("Node 8 Demo") {
          script {
            withJenkins {
              echo "Running some shell Commands ..."
              sh "node --version"
              sh "npm --version"
            }
          }
        }
      }
    }
  }
}
