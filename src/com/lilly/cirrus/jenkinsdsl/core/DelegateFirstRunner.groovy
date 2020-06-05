package com.lilly.cirrus.jenkinsdsl.core

class DelegateFirstRunner {
  static <D, C> C run(D delegate, Closure<C> closure, def args = null)  {

    if(closure) {
      setup(delegate, closure)
      return closure(args)
    }

    return null
  }

  static <D, C> Closure<C> setup(D delegate, Closure<C> closure) {
    if(closure) {
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure.delegate = delegate
    }
    return closure
  }
}
