package com.github.kuhnen.elasticsearch

import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.response.HttpResponse
import sbt.{File, Plugin, settingKey, taskKey}
import sbt._
import sbt.Keys
import sbt.Keys._
import com.stackmob.newman.dsl._
import scala.concurrent._
import scala.concurrent.duration._
import java.net.URL


import scala.util.Try

/**
 * Created by kuhnen on 8/8/14.
 */
object ElasticSearchPlugin extends AutoPlugin {

  lazy val testWithEs = taskKey[Unit]("starts ES before runing the tests")  // in Test <<= test in Test dependsOn(startElasticSearch)

  lazy val testOnlyWithEs = inputKey[Unit]("starts ES before runing specified test")

  lazy val elasticSearchVersion = settingKey[String]("ElasticSearch version")
  lazy val elasticSearchHome = taskKey[File]("Task to create elastic search home directory")
  lazy val deployElasticSearch = taskKey[File]("Task to download (deploy) elastic search")
  lazy val startElasticSearch =  taskKey[Unit]("Task to start elasticSearch")
  lazy val stopElasticSearch =  taskKey[Unit]("Task to stop elasticSearch")
  lazy val elasticSearchConfigFile = settingKey[Option[String]]("elastic search config dir")
  lazy val elasticSearchMappings = settingKey[Option[String]]("Use this to set the initial mapping of elastic search")
  lazy val stopElasticSearchAfterTests = settingKey[Boolean]("stop ES after tests")
  lazy val cleanElasticSearchAfterTests = settingKey[Boolean]("clean ES after tests")
  lazy val elasticSearchFilesToDefaultConfigDir= settingKey[List[String]]("Files to add to the default config directory of ELASTICSEARCH")

  implicit val httpClient = new ApacheHttpClient
  lazy val esPort = settingKey[Int]("Default elastic search port")// 9200

  val elasticSearchSettings = Seq(
    elasticSearchVersion := "1.7.1",
    esPort := 9200, // For now it can not be changed.  We need to find a nice way to get it from the configuration file
    elasticSearchConfigFile := None,
    elasticSearchMappings := None,
    stopElasticSearchAfterTests := true,
    cleanElasticSearchAfterTests := true,
    elasticSearchFilesToDefaultConfigDir := List.empty,
    elasticSearchHome <<=  (elasticSearchVersion, target) map { case (ver, targetDir) => targetDir / s"elasticsearch-${ver}"},
    testWithEs <<= test in Test dependsOn startElasticSearch,
    testOnlyWithEs <<= testOnly in Test dependsOn startElasticSearch,
    //classpathTypes ~= (_ + "tar.gz"),
    //libraryDependencies += {
    //  "org.elasticsearch" % "elasticsearch" % elasticSearchVersion.value artifacts(Artifact("elasticsearch", "tar.gz", "tar.gz"))
    //},
    deployElasticSearch <<= (streams, elasticSearchVersion, target, baseDirectory, elasticSearchFilesToDefaultConfigDir) map {
      case (streams, ver, targetDir, baseDir, confFiles) =>
        val esTar = s"elasticsearch-$ver.tar.gz"
        val downloadPathURL = new URL(s"https://download.elasticsearch.org/elasticsearch/elasticsearch/$esTar")
        val downloadTo = targetDir / "download"
        val esTarFile = downloadTo / esTar
        if (!esTarFile.exists()) {
          streams.log.info(s"Downloading elasticSearch from $downloadPathURL")
          sbt.IO.download(downloadPathURL, esTarFile)
        }
        Process(Seq("tar","-xzf", esTarFile.getAbsolutePath),targetDir).!
        confFiles.foreach { filePath =>
          val source: File = file(filePath)
          val destiny: File = targetDir / s"elasticsearch-${ver}" / "config" / source.getAbsolutePath.split("/").last
          println(s"Copying files: $source to $destiny")
          IO.copyFile(source, destiny)
        }
        targetDir / s"elasticsearch-${ver}"
    },

    startElasticSearch <<= (streams, target, deployElasticSearch, elasticSearchConfigFile, elasticSearchMappings, esPort) map {
      case (streams, targetDir, elasticHome, configDir, esMappings, esPort) =>
        if (!isElasticSearchRunning(esPort)) {
          val confArg = "-Des.config=" + configDir.getOrElse(elasticHome.getAbsolutePath + "/config/elasticsearch.yml")
          streams.log.info(s"configuration used: $confArg")
          val bin = elasticHome / "bin" / "elasticsearch"
          val args = Seq(bin.getAbsolutePath, confArg)
          val proc = Process(args, elasticHome).run
          waitElasticSearch(esPort)
          esMappings.foreach { scriptPath =>
            streams.log.info(s"Initializing ES with $scriptPath")
            val scriptDir: String = scriptPath.split("/").reverse.tail.reverse.mkString("/")
            streams.log.info(s"Script dir: $scriptDir")
            Process(Seq(scriptPath), sbt.file(scriptDir))!
          }
        }
    },

    testOptions in Test <+= (stopElasticSearchAfterTests, cleanElasticSearchAfterTests, esPort) map {
      case (stop, clean, esPort) => Tests.Cleanup(()=> {

          Try {
            if (stop && clean) {
              val url =  new URL(s"http://localhost:$esPort/_all")
              println("[INFO] Deleting all indexes")
              Await.result(DELETE(url).apply, 1.second)
            }
            if (stop) {
              val url = new URL(s"http://localhost:$esPort/_shutdown")
              Await.result(POST(url).apply, 1.second)
            }
          }
        }

      )
    },

      stopElasticSearch <<= (cleanElasticSearchAfterTests, esPort) map {
        case (clean, esPort) => //Try {
          if (clean) {
            val url =  new URL(s"http://localhost:$esPort/_all")
            println("[INFO] Deleting all indexes")
            Await.result(DELETE(url).apply, 1.second)
          }
          val url = new URL(s"http://localhost:$esPort/_shutdown")
          Await.result(POST(url).apply, 1.second)
      //  }
      }

  )


  def waitElasticSearch(port: Int) = {
    //var port = 9200
    var tries = 0
    while(!isElasticSearchRunning(port)){// && tries <= 3){
      tries += 1
      println(s"[[INFO] Waiting for elastic search to be ready. Tried $tries time(s) on port $port")
      Thread.sleep(3000)
    //  if (tries == 3) {
    //    tries = 0
    //    port += 1
      }
    }
  //}

  def isElasticSearchRunning(port: Int) : Boolean = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    implicit val formats = org.json4s.DefaultFormats

    Try {
      val url = new URL(s"http://localhost:$port/_cluster/health?pretty=true")
      val response: HttpResponse = Await.result(GET(url).apply, 1.second)
      println(s"[INFO] ${response.bodyString}")
      val status = (parse(response.bodyString) \\ "status").extract[String]
      status == "green" || status == "yellow"
    }.getOrElse(false)


  }

  def cleanData(home: String) = {

  }
}
