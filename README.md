sbt-elasticsearch-plugin
========================

This is a work in progress project.  The goal is to allow launching [ElasticSearch](http://www.elasticsearch.org/) during tests, and test your application against it.
At this pre-mature phase, only the very basic functionality works (and only on linux/unix). API is not final, and might (probably will) change down the road.
However, the plugin is already usable as is.

## Installation ##
Add the following to your `project/plugins.sbt` file:
```scala
addSbtPlugin("com.github.kuhnen" % "sbt-elasticsearch-plugin" % "0.1-SNAPSHOT")
```

## Usage ##

to start a new elasticsearch before running your tests
```
testWithEs
```
to start a new elasticsearch before running your testOnly
```
testOnlyWithEs
```

### On build.sbt file you can change some configurations ###
 
 ElasticSearchPlugin.elasticSearchVersion := "1.7.1", (override the default ES version)
 
 ElasticSearchPlugin.elasticSearchConfigFile := None, (ES configuration file, if needed)
 
 ElasticSearchPlugin.elasticSearchMappings := None,  (Mappings when starting ElasticSearch)
 
 ElasticSearchPlugin.elasticSearchFilesToDefaultConfigDir := List.empty, (for examples synonimous, and other files that might be needed to ES)
 
 ElasticSearchPlugin.  stopElasticSearchAfterTests := true,
 
 ElasticSearchPlugin.cleanElasticSearchAfterTests := true,
 

Until i'll get this plugin hosted, you can build it yourself, and use `sbt publish-local` to have it available in your local `~/.ivy2`.


