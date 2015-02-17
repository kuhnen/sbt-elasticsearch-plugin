package com.chaordicsystems.platform.sbt.elasticsearch

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
object ElasticSearchPlugin extends Plugin {

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

  val elasticSearchSettings = Seq(
    elasticSearchVersion := "1.3.2",
    elasticSearchConfigFile := None,
    elasticSearchMappings := None,
    stopElasticSearchAfterTests := true,
    cleanElasticSearchAfterTests := true,
    elasticSearchFilesToDefaultConfigDir := List.empty,
    elasticSearchHome <<=  (elasticSearchVersion, target) map { case (ver, targetDir) => targetDir / s"elasticsearch-${ver}"},
    //classpathTypes ~= (_ + "tar.gz"),
    //libraryDependencies += {
    //  "org.elasticsearch" % "elasticsearch" % elasticSearchVersion.value artifacts(Artifact("elasticsearch", "tar.gz", "tar.gz"))
    //},
    deployElasticSearch <<= (streams, elasticSearchVersion, target, baseDirectory, elasticSearchFilesToDefaultConfigDir) map {
      case (streams, ver, targetDir, baseDir, confFiles) =>
        val esTar = s"elasticsearch-$ver.tar.gz"
        val downloadPathURL = new URL(s"https://download.elasticsearch.org/elasticsearch/elasticsearch/$esTar")
        val esTarFile = file(baseDir.toPath + "/" + esTar)
        if (!esTarFile.exists()) {
          streams.log.info(s"Downloading elasticSearch from $downloadPathURL")
          sbt.IO.download(downloadPathURL, file(baseDir.toPath + "/" + esTar))
        }
        Process(Seq("tar","-xzf", esTarFile.getAbsolutePath),targetDir).!
        confFiles.foreach { filePath =>
          val source: File = file(filePath)
          val destiny: File = targetDir / s"elasticsearch-${ver}" / "config" / source.getAbsolutePath.split("/").reverse.head
          println(s"Copying files: $source to $destiny")
          IO.copyFile(source, destiny)
        }
        targetDir / s"elasticsearch-${ver}"
    },

    startElasticSearch <<= (target, deployElasticSearch, elasticSearchConfigFile, elasticSearchMappings) map {
      case (targetDir, elasticHome, configDir, esMappings) =>
        if (!isElasticSearchRunning) {
          val confArg = "-Des.config=" + configDir.getOrElse(elasticHome.getAbsolutePath + "/config/elasticsearch.yml")
          val bin = elasticHome / "bin" / "elasticsearch"
          val args = Seq(bin.getAbsolutePath, confArg)
          val proc = Process(args, elasticHome).run
          waitElasticSearch
          esMappings.foreach { scriptPath =>
            println(s"[INFO] Initializing ES with $scriptPath")
            val scriptDir: String = scriptPath.split("/").reverse.tail.reverse.mkString("/")
            println(s"Script dir: $scriptDir")
            Process(Seq(scriptPath), sbt.file(scriptDir))!
          }

        }

    },

    testOptions in Test <+= (stopElasticSearchAfterTests, cleanElasticSearchAfterTests) map {
      case (stop, clean) => Tests.Cleanup(()=> {

          Try {
            if (stop && clean) {
              val url =  new URL("http://localhost:9200/_all")
              println("[INFO] Deleting all indexes")
              Await.result(DELETE(url).apply, 1.second)
            }
            if (stop) {
              val url = new URL("http://localhost:9200/_shutdown")
              Await.result(POST(url).apply, 1.second)
            }
          }
        }

      )
    },

      stopElasticSearch <<= (cleanElasticSearchAfterTests) map {
        case clean => //Try {
          if (clean) {
            val url =  new URL("http://localhost:9200/_all")
            println("[INFO] Deleting all indexes")
            Await.result(DELETE(url).apply, 1.second)
          }
          val url = new URL("http://localhost:9200/_shutdown")
          Await.result(POST(url).apply, 1.second)
      //  }
      }

  )


  def waitElasticSearch = {
    while(!isElasticSearchRunning){
      println("[[INFO] Waiting for elastic search to be ready")
      Thread.sleep(3000)
    }
  }

  def isElasticSearchRunning: Boolean = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    implicit val formats = org.json4s.DefaultFormats

    Try {
      val url = new URL("http://localhost:9200/_cluster/health?pretty=true")
      val response: HttpResponse = Await.result(GET(url).apply, 1.second)
      println(s"[INFO] ${response.bodyString}")
      val status = (parse(response.bodyString) \\ "status").extract[String]
      status == "green" || status == "yellow"
    }.getOrElse(false)


  }

  def cleanData(home: String) = {

  }
}
