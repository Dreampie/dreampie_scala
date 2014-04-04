package spark

import play.api.Play._
import org.apache.spark.{SparkConf, SparkContext}
import play.api.Logger

/**
 * Created by wangrenhui on 14-4-3.
 */
object SparkManager {
  /*load properties*/
  val SparkKey = "spark."
  val Dot = "."
  private val Plugin = SparkKey + "plugin"
  private val AppName = SparkKey + "appName"
  private val Master = SparkKey + "master"
  private val Memory = SparkKey + "executor.memory"

  var sparkContext: SparkContext = {
    Logger.info("[dreampie]  sparkContext init")
    new SparkContext(loadConf)
  }

  def loadConf: SparkConf = {
    val conf = new SparkConf()
      .setMaster(master)
      .setAppName(appName)
      .set(Memory, memory)
    conf
  }

  /**
   * spark.plugin= enabled/disabled
   * Mode local or network
   */
  lazy val plugin = current.configuration.getString(Plugin).getOrElse("enable")
  /**
   * spark.local= true / false
   * Mode local or network
   */
  lazy val master = current.configuration.getString(Master).getOrElse("local")
  /**
   * spark name
   */
  lazy val appName = current.configuration.getString(AppName).getOrElse("")
  /**
   * spark.executor.memory = 512m / 1g
   * memory size
   */
  lazy val memory = current.configuration.getString(Memory).getOrElse("1g")
}