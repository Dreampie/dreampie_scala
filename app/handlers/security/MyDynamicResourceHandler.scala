package handlers.security

import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import play.api.mvc.Request
import be.objectify.deadbolt.core.DeadboltAnalyzer
import play.Logger

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
class MyDynamicResourceHandler extends DynamicResourceHandler {
  def isAllowed[A](name: String, meta: String, deadboltHandler: DeadboltHandler, request: Request[A]) = {
    //    MyDynamicResourceHandler.handlers(name).isAllowed(name,
    //      meta,
    //      handler,
    //      request)
    val subject = deadboltHandler.getSubject(request)
    if (Logger.isDebugEnabled)
      Logger.debug(s"[dreampie] Get Subject as - %s".format(subject))
    subject match {
      case Some(subject) =>
        //检测是否包含角色
        if (DeadboltAnalyzer.hasRole(subject, name)) {
          true
        } else {
          false
          // a call to view profile is probably a get request, so
          // the query string is used to provide info
          // See the Deadbolt documentation on why this is harder to do with path parameters
          //          Map queryStrings = context.request().queryString();
          //          String[] requestedNames = queryStrings.get("userName");
          //          allowed = requestedNames != null
          //          && requestedNames.length == 1
          //          &&((AuthorisedUser) subject).userName.equals(requestedNames[ 0] );
        }
      case _ => false
    }
  }

  def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler, request: Request[A]) = {
    // todo implement this when demonstrating permissions
    val subject = deadboltHandler.getSubject(request)
    if (Logger.isDebugEnabled)
      Logger.debug(s"[dreampie] Get Subject as - %s".format(subject))
    subject match {
      case Some(subject) =>
        //check permission
        if (DeadboltAnalyzer.checkPatternEquality(subject, permissionValue)) {
          true
        } else {
          false
        }
      case _ => false
    }
  }
}

//object MyDynamicResourceHandler {
//  val handlers: scala.collection.mutable.Map[String, DynamicResourceHandler] =
//    scala.collection.mutable.Map(
//      "pureLuck" -> new DynamicResourceHandler() {
//        def isAllowed[A](name: String, meta: String, deadboltHandler: DeadboltHandler, request: Request[A]) =
//          System.currentTimeMillis() % 2 == 0
//
//        def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler, request: Request[A]) = false
//      }
//    )
//  handlers += (
//    "testLuck" -> new DynamicResourceHandler() {
//      override def isAllowed[A](name: String, meta: String, deadboltHandler: DeadboltHandler, request: Request[A]): Boolean = true
//
//      override def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler, request: Request[A]): Boolean = true
//    }
//    )
//}