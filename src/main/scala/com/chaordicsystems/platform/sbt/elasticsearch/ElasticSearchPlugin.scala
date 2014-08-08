package com.chaordicsystems.platform.sbt.elasticsearch

import sbt.{File, Plugin, settingKey, taskKey}
import sbt._
import sbt.Keys
import Keys.{target, libraryDependencies, dependencyClasspath, classpathTypes}


/**
 * Created by kuhnen on 8/8/14.
 */
object ElasticSearchPlugin extends Plugin {

  lazy val elasticSearchVersion = settingKey[String]("ElasticSearch version")
  lazy val elasticSearchHome = taskKey[File]("Task to create elastic search home directory")
  lazy val deployElasticSearch = taskKey[File]("Task to download (deploy) elastic search")
  lazy val startElasticSearch =  taskKey[Unit]("Task to start elasticSearch")

  val elasticSearchSettings = Seq(
    elasticSearchVersion := "1.3.1",
    elasticSearchHome <<=  (elasticSearchVersion, target) map { case (ver, targetDir) => targetDir / s"elasticsearch-${ver}"},
    classpathTypes ~= (_ + "tar.gz"),
    libraryDependencies += {
      "org.elasticsearch" % "elasticsearch" % elasticSearchVersion.value artifacts(Artifact("elasticsearch", "tar.gz", "tar.gz")) intransitive()
    },
    deployElasticSearch <<= (elasticSearchVersion, target, dependencyClasspath in Runtime) map {
      case (ver, targetDir, classpath) =>
        val elasticSearchTarGz: Option[File] = Attributed.data(classpath).find(_.getName == s"elasticsearch-$ver.tar.gz")
        if (elasticSearchTarGz.isEmpty) sys.error("could not load: elasticsearch tar.gz file")
        println(s"elasticSearch tar.gz path: ${elasticSearchTarGz.get.getAbsolutePath}")
        Process(Seq("tar","-xzf",elasticSearchTarGz.get.getAbsolutePath),targetDir).!
        targetDir / s"elasticsearch-${ver}"
    },
    startElasticSearch <<= (target, deployElasticSearch) map {
      case (targetDir, elasticHome) =>
        val pidFile = targetDir / "es.pid"
        val bin = elasticHome / "bin" / "elasticsearch"
        val args = Seq(bin.getAbsolutePath)
        val proc = Process(args, elasticHome).run
        println(proc)

    }

  )

}
