package com.lilly.cirrus.jenkinsdsl.utility

class GitUtilsSpec extends spock.lang.Specification {

  def "the repo name can be extracted from a url in the git utility"(String url, String name) {
    given: "nothing"
    expect: "the repo name is parsed from the url"
    GitUtils.getRepoNameFromURL(url) == name

    where: "the input is as follows"
    url                                   | name
    "https://some.host.name/RepoName.git" | "RepoName"
    "https://some.host.name/RepoName"     | "RepoName"
  }
}
