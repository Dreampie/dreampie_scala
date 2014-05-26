//import index.IndexHelpers.{IndexResults, IndexQuery}

import filters.LoggingFilter
import index.IndexManager
import org.elasticsearch.common.Priority
import org.joda.time._

import models.security._
import play.api._
import play.api.mvc._
import secure.AuthenticationMethod
import index.IndexManager._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mapping.FieldType._
import scala.concurrent.Await
import com.sksamuel.elastic4s.ElasticDsl._

object Global extends GlobalSettings {

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(request)
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), LoggingFilter)
  }

  override def onStart(app: Application) {
    InitialData.init()
  }

}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {

  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

  def init() {
    data()
    index()
  }

  //初始化数据
  private def data() = {

    if (User.countAll <= 0) {
      Seq(
        User(1, "admin", "dreampie_scala", null, null, "$2a$10$q5IvwSTS4XNA025F9ScCt.tTaavvdN6BgLjqDxZssxXhDP4YU/Tpu",
          "BCryptHasher", "$2a$10$q5IvwSTS4XNA025F9ScCt", null, "王", "", "", DateTime.now(), null, null, authMethod = AuthenticationMethod.UserPassword)
      ).foreach {
        user => User.create(user.username, user.providername, user.email, user.mobile, user.password, user.hasher,
          user.salt, user.avatarUrl, user.firstName, user.lastName, user.fullName, user.createdAt).addRoleId(1)
          UserInfo.create(user.id, Option(0), 0, DateTime.now)
      }
    }
    if (Role.countAll <= 0) {
      Seq(Role(1, "admin", "admin", null, DateTime.now, null, null)).foreach {
        role => Role.create(role.name, role.value, role.intro, role.createdAt).addPermissionId(1)
      }
    }

    if (Permission.countAll <= 0) {
      Seq(Permission(1, "基础权限", "base", Option("/**"), null, DateTime.now, null, null)).foreach {
        permission => Permission.create(permission.name, permission.value, permission.url, permission.intro, permission.createdAt)
      }
    }
  }

  //初始化数据索引
  private def index() = {
    IndexManager.init()
    User.findAll.foreach {
      user =>
        add("user" -> "user", user)
    }
    IndexManager.refreshAll("user")
    IndexManager.prepareHealth(Priority.LANGUID)
    val resp = IndexManager.searchBy("user" -> "user", "username" -> "admin")
    println("[dreampie].........................search user" + resp.getHits.totalHits())
  }
}