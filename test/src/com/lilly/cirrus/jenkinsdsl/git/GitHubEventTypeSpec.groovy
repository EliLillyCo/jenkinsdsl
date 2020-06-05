package com.lilly.cirrus.jenkinsdsl.git

import com.lilly.cirrus.jenkinsdsl.sim.CirrusDSLSpec

class GitHubEventTypeSpec extends CirrusDSLSpec {
  def "toString produces string rep for fields"() {
    given: "A gitHub event type object"
    GitHubEventType eventType = new GitHubEventType(
      isRelease: true,
    )

    when: "the toString is called"
    String result = eventType.toString()

    then: "all fields value are returned"
    result.contains('isPull=false')
    result.contains('isPullOnMaster=false')
    result.contains('isPullOnDevelop=false')
    result.contains('isRelease=true')
    result.contains('isPreRelease=false')
    result.contains('isPushToMaster=false')
    result.contains('isPullRequestMergeToMaster=false')
    result.contains('isPullRequestMergeToDevelop=false')
  }
}
