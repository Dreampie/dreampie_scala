package handlers.security

import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import play.api.mvc._
import be.objectify.deadbolt.core.models.Subject
import scala.concurrent.Future
import scala.Some
import views.html._
import play.Logger
import secure.Secure

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
class MyDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    if (dynamicResourceHandler.isDefined) dynamicResourceHandler
    else Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    // e.g. request.session.get("secure")
    val authenticator = Secure.authenticatorFromRequest(request)

    authenticator match {
      case Some(authenticator) =>
        val u = Some(authenticator.user)
        u match {
          case Some(u) =>
            if (Logger.isDebugEnabled) {
              Logger.debug(s"[dreampie] Getting subject as - ${u.username}")
            }
            Some(u)
          case _ => None
        }
      case _ => None
    }

  }

  override def onAuthFailure[A](request: Request[A]): Future[SimpleResult] = {
    if (Logger.isDebugEnabled)
      Logger.debug("[dreampie] Authentication failure")
    Future.successful(Results.Forbidden(accessFailed()))
  }
}