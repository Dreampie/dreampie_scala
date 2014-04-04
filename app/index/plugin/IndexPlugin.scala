package index.plugin

import play.api._
import org.elasticsearch.common.settings._
import play.api.Logger
import play.api.Play._
import scala.collection.immutable.List
import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import com.sksamuel.elastic4s.ElasticClient

/**
 * Created by wangrenhui on 14-3-20.
 */
class IndexPlugin(application: Application) extends Plugin {

  import index.plugin.IndexPlugin._

  def isPluginDisabled: Boolean = {
    plugin != None && plugin.equals("disabled")
  }

  override def enabled(): Boolean = {
    !isPluginDisabled
  }


  override def onStart() = {
    Logger.info("[dreampie] loaded index plugin: index.IndexPlugin")
    // Load Elasticsearch Settings
    val settings = loadSettings

    // Check Model
    if (local) {
      Logger.info("ElasticSearch : Starting in Local Mode")

      client = ElasticClient.local(settings)
      Logger.info("ElasticSearch : Started in Local Mode")
    } else {
      Logger.info("ElasticSearch : Starting in Client Mode")
      if (clientAddresses == None || clientAddresses.length <= 0) {
        throw new Exception("Configuration required - elasticsearch.client when local model is disabled!")
      }

      var done: Boolean = false
      var addresses = Map[String, Int]()

      clientAddresses.foreach {
        host => val parts = host.split(":")
          if (parts.length != 2) {
            throw new Exception("Invalid Host: " + host)
          }
          Logger.info(s"ElasticSearch : Client - Host: %s Port: %s".format(parts(0), parts(1)))
          addresses += (parts(0) -> parts(1).toInt)
          done = true
      }
      if (!done) {
        throw new Exception("No Hosts Provided for ElasticSearch!")
      }

      client = ElasticClient.remote(settings, addresses.map {
        case (k, v) => (k, v)
      }.toSeq: _*)
      Logger.info("ElasticSearch : Started in Client Mode")
    }

    // Check Client
    if (client == null) {
      throw new Exception("ElasticSearch Client cannot be null - please check the configuration provided and the health of your ElasticSearch instances.")
    }

  }

  /**
   * Load settings from resource file
   *
   * @return
   * @throws Exception
   */
  def loadSettings: Settings = {
    val settings: ImmutableSettings.Builder = ImmutableSettings.settingsBuilder()

    // set default settings
    settings.put("client.transport.sniff", sniff)
    // load settings
    if (localConfig != None) {
      Logger.debug("Elasticsearch : Load settings from " + localConfig)
      try {
        settings.loadFromClasspath(localConfig.toString)
      } catch {
        case e: SettingsException =>
          Logger.error("Elasticsearch : Error when loading settings from %s".format(localConfig))
          throw new Exception(e)
      }
    }
    Logger.info("Elasticsearch : Settings  %s".format(settings.internalMap().toString))
    settings.build()
  }

  override def onStop() = {

    if (client != null) {
      // Deleting index(s) if define in conf
      if (dropOnShutdown) {
        val listener = client.client.admin().indices().prepareRefresh("_all").execute()
        listener.actionGet()
      }
      // Stopping the client
      if (client != null) {
        client.close()
      }
    }
    Logger.info("ElasticSearch : Plugin has stopped");
  }
}

object IndexPlugin {
  /*load properties*/
  val ElasticsearchKey = "elasticsearch."
  val Dot = "."
  private val Plugin = ElasticsearchKey + "plugin"
  private val Local = ElasticsearchKey + "local"
  private val Sniff = ElasticsearchKey + "client.transport.sniff"
  private val LocalConfig = Local + "config"
  private val ClientAddresses = ElasticsearchKey + "client.addresses"
  private val ShowRequest = ElasticsearchKey + "showRequest"
  private val DropOnShutdown = ElasticsearchKey + "dropOnShutdown"

  var client: ElasticClient = null
  /**
   * elasticsearch.plugin= enabled/disabled
   * Mode local or network
   */
  lazy val plugin = current.configuration.getString(Plugin).getOrElse(None)
  /**
   * elasticsearch.local= true / false
   * Mode local or network
   */
  lazy val local = current.configuration.getBoolean(Local).getOrElse(false)

  /**
   * elasticsearch.local.config = configuration file load on local mode.
   * eg : conf/elasticsearch.yml
   */
  lazy val localConfig = current.configuration.getString(LocalConfig).getOrElse(None)
  /**
   * elasticsearch.client.sniff = true / false
   * Sniff for nodes.
   */
  var sniff: Boolean = current.configuration.getBoolean(Sniff).getOrElse(true)

  /**
   * elasticsearch.client = list of client separate by commas ex : 192.168.0.1:9300,192.168.0.2:9300
   */
  lazy val clientAddresses: List[String] = current.configuration.getStringList(ClientAddresses).map(_.toList).getOrElse(List())

  /**
   * Debug mode for log search request and response
   */
  lazy val showRequest = current.configuration.getBoolean(ShowRequest).getOrElse(false)

  /**
   * Drop the index on application shutdown
   * Should probably be used only in tests
   */
  lazy val dropOnShutdown = current.configuration.getBoolean(DropOnShutdown).getOrElse(false)
}