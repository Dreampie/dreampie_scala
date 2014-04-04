package index

import index.plugin._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._
import scala.concurrent.{Future, Await}
import org.elasticsearch.indices.IndexMissingException
import scala.concurrent.duration._
import com.sksamuel.elastic4s.mapping.MappingDefinition
import com.sksamuel.elastic4s.source.{JacksonSource, ObjectSource}
import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.common.Priority
import org.elasticsearch.action.{ActionRequestBuilder, ActionResponse, ActionRequest}
import org.elasticsearch.action.search.SearchResponse
import play.api.Logger

/**
 * Created by wangrenhui on 14-3-20.
 */
object IndexManager {
  val client: ElasticClient = IndexPlugin.client
  val mapper = new ObjectMapper()

  def init() {
    val exist = exists("user")
    Logger.info("[dreampie] %s index is exist as %s".format("user", exist))
    if (!exist) {
      client.sync.execute {
        create index "user" shards 1 replicas 0 mappings (
          "user" as(
            id typed LongType,
            "username" typed StringType analyzer KeywordAnalyzer,
            "providername" typed StringType analyzer KeywordAnalyzer,
            "email" typed StringType analyzer KeywordAnalyzer,
            "fullName" typed StringType analyzer KeywordAnalyzer,
            "createAt" typed DateType,
            "userInfo" nested(
              "gender" typed IntegerType,
              "createAt" typed DateType
              )
            ) size true numericDetection true boostNullValue 1.2 boost "myboost"
          )
      }
    }
  }

  def exists(indexes: String*): Boolean = {
    client.sync.exists(indexes.toSeq: _*).isExists
  }

  def add(indexType: (String, String), fields: (String, Any)*): Future[ActionResponse] = {
    client.execute {
      index into indexType fields (fields)
    }
  }

  def add(indexType: (String, String), obj: Object): Future[ActionResponse] = {
    client.execute {
      index into indexType doc ObjectSource(obj)
    }
  }

  def add(indexType: (String, String), json: String): Future[ActionResponse] = {
    val jsonObj = mapper.readTree(json)
    client.execute {
      index into indexType doc JacksonSource(jsonObj)
    }
  }


  def deleteAll(indexes: String*): Future[ActionResponse] = {
    client.execute {
      delete index (indexes.toSeq: _*)
    }
  }

  def deleteBy(indexTypes: IndexesTypes, where: String): Future[ActionResponse] = {
    client.execute {
      delete from indexTypes where where
    }
  }

  def deleteBy(indexTypes: IndexesTypes, where: QueryDefinition): Future[ActionResponse] = {
    client.execute {
      delete from indexTypes where where
    }
  }

  def searchAll(indexType: (String, String)): SearchResponse = {
    client.sync.execute {
      search in indexType
    }
  }

  def searchBy(indexType: (String, String), query: String): SearchResponse = {
    client.sync.execute {
      search in indexType query query
    }
  }

  def searchBy(indexType: (String, String), query: QueryDefinition): SearchResponse = {
    client.sync.execute {
      search in indexType query query
    }
  }


  def updateBy(id: Any, indexType: (String, String), fields: (String, Any)*): Future[ActionResponse] = {
    client.execute {
      update(id).in(indexType).doc(fields)
    }
  }

  def valid(indexType: (String, String)): Boolean = {
    client.sync.execute {
      validate in indexType
    }.isValid
  }

  def validBy(indexType: (String, String), query: String): Boolean = {
    client.sync.execute {
      validate in indexType query query
    }.isValid
  }

  def validBy(indexType: (String, String), query: QueryDefinition): Boolean = {
    client.sync.execute {
      validate in indexType query query
    }.isValid
  }

  def refreshAll(indexes: String*) {
    val i = indexes.size match {
      case 0 => Seq("_all")
      case _ => indexes
    }
    client.client.admin().indices().prepareRefresh(i: _*).execute().actionGet()
  }

  def countAll(indexTypes: IndexesTypes): Long = {
    client.sync.execute {
      count from indexTypes
    }.getCount
  }

  def countBy(indexTypes: IndexesTypes, where: String): Long = {
    client.sync.execute {
      count from indexTypes where where
    }.getCount
  }

  def countBy(indexTypes: IndexesTypes, where: QueryDefinition): Long = {
    client.sync.execute {
      count from indexTypes where where
    }.getCount
  }

  def prepareHealth(waitForEvents: Priority) {
    client.admin.cluster.prepareHealth().setWaitForEvents(waitForEvents).setWaitForGreenStatus().execute().actionGet
  }

  def optimizeAll(indexes: String*): Future[ActionResponse] = {
    client.execute {
      optimize index indexes
    }
  }
}
