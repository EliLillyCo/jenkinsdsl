package com.lilly.cirrus.jenkinsdsl.sim

import org.spockframework.lang.Wildcard

class SimFactory {
  static CommandSim any() {
    new CommandSim(type: Wildcard.INSTANCE, arguments: Wildcard.INSTANCE)
  }

  static CommandSim sh(def command) {
    new CommandSim(type: "sh", arguments: [script: command])
  }

  static CommandSim sh(Map args) {
    new CommandSim(type: "sh", arguments:  args)
  }

  static CommandSim dir(String path, Closure<?> body) {
    new CommandSim(type:  "dir", arguments: [path: path], body: body)
  }

  static CommandSim echo(def text) {
    new CommandSim(type: "echo", arguments: [text: text])
  }

  static CommandSim timeout(Map args) {
    new CommandSim(type: "timeout", arguments: args)
  }

  static CommandSim error(def text) {
    new CommandSim(type: "error", arguments: [text: text])
  }

  static CommandSim evaluate(def text) {
    new CommandSim(type: "evaluate", arguments: [text: text])
  }

  static CommandSim stash(Map args) {
    new CommandSim(type: "stash", arguments:  args)
  }

  static CommandSim stash(def args) {
    new CommandSim(type: "stash", arguments:  args)
  }

  static CommandSim unstash(String name) {
    new CommandSim(type: "unstash", arguments: [name: name])
  }

  static CommandSim unstash(def name) {
    new CommandSim(type: "unstash", arguments: [name: name])
  }

  static CommandSim checkout(def scm) {
    new CommandSim(type:  "checkout", arguments: [scm: scm])
  }

  static CommandSim stage(def name, Closure<?> body = {}) {
    new CommandSim(type:  "stage", arguments: [name: name], body: body)
  }

  static CommandSim parallel(Map... maps) {
    Map finalMap = [:]
    for(Map m: maps) {
      finalMap = finalMap.plus(m)
    }

    new ParallelCommandSim(arguments: finalMap)
  }

  static CommandSim containerTemplate(Map args) {
    new CommandSim(type: "containerTemplate", arguments: args)
  }

  static CommandSim containerTemplate(def args) {
    new CommandSim(type: "containerTemplate", arguments: args)
  }

  static CommandSim podTemplate(Map args, Closure<?> body = {}) {
    new CommandSim(type: "podTemplate", arguments: args, body: body)
  }

  static CommandSim podTemplate(def args, Closure<?> body = {}) {
    new CommandSim(type: "podTemplate", arguments: args, body: body)
  }

  static CommandSim node(def label, Closure<?> body = {}) {
    new CommandSim(type: "node", arguments: [label: label], body: body)
  }

  static CommandSim container(def name, Closure<?> body = {}) {
    new CommandSim(type: "docker", arguments: [name: name], body: body)
  }

  static CommandSim withCredentials(List credentials, Closure<?> body = {}) {
    new CommandSim(type: "withCredentials", arguments: [credentials: credentials], body: body)
  }

  static CommandSim withCredentials(String credentials, Closure<?> body = {}) {
    new CommandSim(type: "openshift.withCredentials", arguments: [credentials: credentials], body: body)
  }

  static CommandSim withCredentials(def credentials, Closure<?> body = {}) {
    new CommandSim(type: "withCredentials", arguments: [credentials: credentials], body: body)
  }

  static CommandSim usernamePassword(Map args) {
    new CommandSim(type: "usernamePassword", arguments: args)
  }

  static CommandSim usernamePassword(def args) {
    new CommandSim(type: "usernamePassword", arguments: args)
  }

  static CommandSim usernameColonPassword(Map args) {
    new CommandSim(type: "usernameColonPassword", arguments: args)
  }

  static CommandSim usernameColonPassword(def args) {
    new CommandSim(type: "usernameColonPassword", arguments: args)
  }

  static CommandSim certificate(Map args) {
    new CommandSim(type: "certificate", arguments: args)
  }

  static CommandSim certificate(def args) {
    new CommandSim(type: "certificate", arguments: args)
  }

  static CommandSim string(Map args) {
    new CommandSim(type: "string", arguments: args)
  }

  static CommandSim string(def args) {
    new CommandSim(type: "string", arguments: args)
  }

  static CommandSim file(Map args) {
    new CommandSim(type: "file", arguments: args)
  }

  static CommandSim file(def args) {
    new CommandSim(type: "file", arguments: args)
  }

  static CommandSim create(String... list) {
    new CommandSim(type: "openshift.create", arguments: [list: list.toList()])
  }

  static CommandSim create(List list) {
    new CommandSim(type: "openshift.create", arguments: [list: list])
  }

  static CommandSim create(def list) {
    new CommandSim(type: "openshift.create", arguments: [list: list])
  }

  static CommandSim secrets(String... list) {
    new CommandSim(type: "openshift.secrets", arguments: [list: list.toList()])
  }

  static CommandSim secrets(List list) {
    new CommandSim(type: "openshift.secrets", arguments: [list: list])
  }

  static CommandSim secrets(def list) {
    new CommandSim(type: "openshift.secrets", arguments: [list: list])
  }

  static CommandSim raw(String... list) {
    new CommandSim(type: "openshift.raw", arguments: [list: list.toList()])
  }

  static CommandSim raw(List list) {
    new CommandSim(type: "openshift.raw", arguments: [list: list])
  }

  static CommandSim raw(def list) {
    new CommandSim(type: "openshift.raw", arguments: [list: list])
  }

  static CommandSim selector(String... list) {
    new CommandSim(type: "openshift.selector", arguments: [list: list.toList()])
  }

  static CommandSim selector(def list) {
    new CommandSim(type: "openshift.selector", arguments: [list: list])
  }

  static CommandSim delete(String... list) {
    new CommandSim(type: "openshift.delete", arguments: [list: list.toList()])
  }

  static CommandSim delete(def list) {
    new CommandSim(type: "openshift.delete", arguments: [list: list])
  }

  static CommandSim delete() {
    new CommandSim(type: "openshift.delete")
  }

  static CommandSim startBuild(String... list) {
    new CommandSim(type: "openshift.startBuild", arguments: [list: list.toList()])
  }

  static CommandSim startBuild(def list) {
    new CommandSim(type: "openshift.startBuild", arguments: [list: list])
  }

  static CommandSim describe() {
    new CommandSim(type: "openshift.describe")
  }

  static CommandSim exists() {
    new CommandSim(type: "openshift.exists")
  }

  static CommandSim withCluster(Closure body = {}) {
    new CommandSim(type: "openshift.withCluster", body: body)
  }

  static CommandSim withProject(String name, Closure body = {}) {
    new CommandSim(type: "openshift.withProject", arguments: [name: name], body: body)
  }

  static CommandSim withCluster(String url, String token, Closure body = {}) {
    new CommandSim(type: "openshift.withCluster", arguments: [url: url, token: token], body: body)
  }

  static CommandSim withProject(def name, Closure body = {}) {
    new CommandSim(type: "openshift.withProject", arguments: [name: name], body: body)
  }

  static CommandSim rollout() {
    new CommandSim(type: "openshift.rollout")
  }

  static CommandSim latest() {
    new CommandSim(type: "openshift.latest")
  }

  static CommandSim readJSON(Map args) {
    new CommandSim(type: "readJSON", arguments: args)
  }

  static CommandSim readJSON(def args) {
    new CommandSim(type: "readJSON", arguments: args)
  }

  static CommandSim readYaml(Map args) {
    new CommandSim(type: "readYaml", arguments: args)
  }

  static CommandSim readYaml(def args) {
    new CommandSim(type: "readYaml", arguments: args)
  }

  static CommandSim readTrusted(def path) {
    new CommandSim(type: "readTrusted", arguments: [path: path])
  }

  static CommandSim readFile(Map args) {
    new CommandSim(type: "readFile", arguments: args)
  }

  static CommandSim readFile(def args) {
    if (args instanceof String || args instanceof GString)
      return new CommandSim(type: "readFile", arguments: [file: args])

    new CommandSim(type: "readFile", arguments: args)
  }

  static CommandSim writeFile(Map args) {
    new CommandSim(type: "writeFile", arguments: args)
  }

  static CommandSim writeFile(def args) {
    if (args instanceof String || args instanceof GString)
      return new CommandSim(type: "writeFile", arguments: [file: args])
    new CommandSim(type: "writeFile", arguments: args)
  }

  static CommandSim fileExists(def path) {
    new CommandSim(type: 'fileExists', arguments: [path: path])
  }

  static CommandSim touch(def path) {
    new CommandSim(type: 'touch', arguments: [path: path])
  }

  static CommandSim load(def path){
    new CommandSim(type: 'load', arguments: [path: path])
  }

  static CommandSim zip(Map args) {
    new CommandSim(type: 'zip', arguments: args)
  }

  static CommandSim zip(def args) {
    new CommandSim(type: 'zip', arguments: args)
  }

  static CommandSim libraryResource(def path) {
    new CommandSim(type: "libraryResource", arguments: [path: path])
  }

  static CommandSim archiveArtifacts(Map args) {
    new CommandSim(type: "archiveArtifacts", arguments: args)
  }

  static CommandSim archiveArtifacts(def args) {
    new CommandSim(type: "archiveArtifacts", arguments: args)
  }
}
