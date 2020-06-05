package com.lilly.cirrus.jenkinsdsl.git

class GitHubEventType {
  def isPull                      = false
  def isPullOnMaster              = false
  def isPullOnDevelop             = false
  def isRelease                   = false
  def isPreRelease                = false
  def isPushToMaster              = false
  def isPullRequestMergeToMaster  = false
  def isPullRequestMergeToDevelop = false
  String branchBeingReleased      = ''

  @Override
  public String toString() {
    return "GitHubEventType {" +
      "isPull=" + isPull +
      ", isPullOnMaster=" + isPullOnMaster +
      ", isPullOnDevelop=" + isPullOnDevelop +
      ", isRelease=" + isRelease +
      ", isPreRelease=" + isPreRelease +
      ", isPushToMaster=" + isPushToMaster +
      ", isPullRequestMergeToMaster=" + isPullRequestMergeToMaster +
      ", isPullRequestMergeToDevelop=" + isPullRequestMergeToDevelop +
      ", branchBeingReleased= " + (branchBeingReleased ?: 'none') +
      '}';
  }

}
