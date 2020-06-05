# Jenkins Pipeline Simulator

The goal of this project is to create an easy to use yet robust unit testing framework that takes into consideration 
the functions that are called from jenkins itself (`sh`, `echo`, `fileExists`, etc.). These provide issues when testing
our Shared Libraries since they do not resolve to anything inside of our code. In order to change this, we have created
this Pipeline Simulator package that allows for us to mock the internal Jenkins calls. 

## How it works
The `JenkinsSim` class handles all of the different functions that could be called upon from the Jenkins Plugins. This
`JenkinsSim` class creates an object that can be used in place for full end to end testing in our unit tests of different
pipeline stages and even full end to end testing of pipelines. For example, here is a simple test case from our `ShellStageSpec`.

```groovy
  def "a shell stage should echo the supplied command during dry run"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = new JenkinsSim()
    setupScope(scope, jenkinsSim)
    scope.dryRun = true

    String command = "ls -lah"
    ShellStage stage = ShellStage.Builder.create()
      .withCommand("ls -lah")
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should execute the shell command"
    jenkinsSim >> SimFactory.echo(command)
  }
```
As you can see in this basic example, we create a stage with a specific command that we want to run. This command however
is being passed into an `echo` call to Jenkins and not something that we would have access to in any typical build process.
By creating a `Scope` and attaching our `JenkinsSim` to it, we can run stages in that `scope` which have access to our 
`jenkinsSim`. When the `then:` block is called, jenkinsSim checks to make sure that `echo` was called on it and that its 
contents were `command`. 

To see how we can also use this to mock values from these calls, here is an example of that:
```groovy
  def "npm test should handle error with test gracefully"() {
    given: "a scope configured with a pipeline and a shell stage configured with a command"
    Scope scope = new Scope()
    JenkinsSim jenkinsSim = new JenkinsSim()
    setupScope(scope, jenkinsSim)
    
    jenkinSim.when(SimFactory.sh(script: {script -> script.contains('npm test')}, returnStatus: true)).then {
        return 1 // This is equivalent to failing a test stage which would normally return 0 if successful
    }

    Stage stage = NpmTestStage.Builder.create()
      .build()

    when: "executing the shell stage"
    stage.execute(scope)

    then: "it should execute the shell command"
    jenkinsSim >> SimFactory.sh(script: {script -> script.contains('npm test')}) +
        SimFactory.echo('NPM Test stage failed with status code: 1')
  }
```

## Limitations
- Parallel steps are not supported with this simulator so any code will run in the order it is given. All parallel steps 
are just put into an exact order and then ran in serial.
- The code inside of these calls is not actually executed. This means that if there is a call where the return values matter
and you are looking to test Groovy code surrounding that block, you are going to have to mock its response. If no response
is needed, you can simply not mock it and it will return `null`.
- Since the code is not executed, any possible returns for that script will need to be mocked manually. For example,
if your shell script is checking to see if a file exists, the simulator will not be able to know how to execute that. Instead,
you are going to have to manually handle a case like that. 
