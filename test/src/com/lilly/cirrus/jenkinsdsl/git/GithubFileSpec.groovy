package com.lilly.cirrus.jenkinsdsl.git

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec
import com.lilly.cirrus.jenkinsdsl.core.CirrusPipelineException
import com.lilly.cirrus.jenkinsdsl.core.Scope
import com.lilly.cirrus.jenkinsdsl.sim.PipelineMock
import com.lilly.cirrus.jenkinsdsl.sim.JenkinsSim
import com.lilly.cirrus.jenkinsdsl.sim.SimFactory

class GithubFileSpec extends CirrusDSLSpec {
  def "merge path should merge the given path"(String path, String fileName, String finalPath) {
    expect:
    finalPath == GithubFile.mergePath(path, fileName)

    where:
    path        |   fileName    |   finalPath
    null        |   'file'      |   'file'
    '.'         |   'file'      |   'file'
    ''          |   'file'      |   'file'
    './'        |   'file'      |   'file'
    '/'         |   'file'      |   'file'
    'abc/def'   |   'file'      |   'abc/def/file'
  }

  def "get repo base should return the repo base"() {
    given: "A scope and pipeline configured with the sh command"
    Scope scope = new Scope()
    scope.ok = true
    JenkinsSim pipelineSim = createJenkinsSim()
    pipelineSim.scm.userRemoteConfigs = [[url: "https://github.com/EliLillyCo/CIRR_Jenkins_DSL.git"]]
    scope.pipeline = pipelineSim

    when: "a github file is created with the pipeline"
    GithubFile githubFile = new GithubFile(pipeline: pipelineSim, filePath: "package.json")

    then: "The stage should be able to retrieve the repo base correctly"
    githubFile.getRepoBase() == "EliLillyCo/CIRR_Jenkins_DSL"
  }

  def "On executing read, the github file should read the content of the remote file"() {
    given: "A scope and pipeline configured with the sh command"
    Scope scope = new Scope()
    scope.ok = true

    JenkinsSim pipelineSim = createJenkinsSim()
    pipelineSim.scm.userRemoteConfigs = [[url: "https://github.com/EliLillyCo/CIRR_Jenkins_DSL.git"]]
    pipelineSim.env.BRANCH_NAME = "test"
    pipelineSim.withCredentialBinding("github-secret-text", [gitHubToken: "token"])

    scope.pipeline = pipelineSim
    scope.env = pipelineSim.env

    String curlString = "curl -H \"Authorization: token token\" " +
      "-L https://raw.githubusercontent.com/EliLillyCo/CIRR_Jenkins_DSL/test/package.json"

    String jsonString = """
     {
       "name": "test",
       "engines": {
         "node": "10.16.0"
       }
     } 
     """.trim()

    pipelineSim.when(SimFactory.sh(script: "${curlString}", returnStdout: true)).then {
      return jsonString
    }

    when: "a Github file is read"
    GithubFile githubFile = new GithubFile(pipeline: pipelineSim, filePath: "package.json")

    then: "The stage should be able to retrieve the repo base correctly"
    githubFile.read() == jsonString
  }

  def "Github file should not work without a pipeline object"() {
    when: "a github file is read without a pipeline object"
    GithubFile githubFile = new GithubFile(filePath: "abc")
    githubFile.read()

    then: "it should throw an exception"
    thrown(CirrusPipelineException)
  }

  def "Github file should not work without a file path to read"() {
    when: "a github file is read without a providing a file path"
    GithubFile githubFile = new GithubFile(pipeline: Mock(PipelineMock))
    githubFile.read()

    then: "it should throw an exception"
    thrown(IOException)
  }

  def "the github file should use the changed branched if exists instead of branch name when reading file"() {
    given: "pipeline with a PR"
    JenkinsSim pipelineSim = createJenkinsSim()
    pipelineSim.env.BRANCH_NAME = "PR-1"
    pipelineSim.env.CHANGE_BRANCH = "feature"
    GithubFile githubFile = new GithubFile(pipeline: pipelineSim)

    when: "get branch name is called"
    String branch = githubFile.getBranchName()

    then: "it should return the change_branch"
    branch == "feature"
  }

  def "transform applies the given binding to the contents"(String contents, String result) {
    given: "a github file with bindings and a sample content"
    GithubFile file = new GithubFile()
    file.bindings = ['fruit': 'apple', 'veggies': 'onions']

    expect:
    result == file.transform(contents)

    where:
    contents                          |   result
    'fruit is fruit not veggies'      |   'apple is apple not onions'
    'veggies are veggies not fruit'   |   'onions are onions not apple'
    'veggies are unique'              |   'onions are unique'
    'people may not like them'        |   'people may not like them'
  }
}
