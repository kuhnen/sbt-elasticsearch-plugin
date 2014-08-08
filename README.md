sbt-elasticsearch-plugin
========================

This is a work in progress project.  The goal is to allow launching [ElasticSearch](http://www.elasticsearch.org/) during tests, and test your application against it.
At this pre-mature phase, only the very basic functionality works (and only on linux/unix). API is not final, and might (probably will) change down the road.
However, the plugin is already usable as is.

## Installation ##
Add the following to your `project/plugins.sbt` file:
```scala
addSbtPlugin("com.chaordicsystems.platform" % "sbt-elasticsearch-plugin" % "0.1-SNAPSHOT")
```
Until i'll get this plugin hosted, you can build it yourself, and use `sbt publish-local` to have it available in your local `~/.ivy2`.


