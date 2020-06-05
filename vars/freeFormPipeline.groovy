#!/usr/bin/env groovy
import com.lilly.cirrus.jenkinsdsl.core.Pipeline

def call(Closure<?> body) {
  Pipeline pipeline = new Pipeline()
  pipeline.start(this, body)
}
