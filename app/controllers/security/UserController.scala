package controllers.security

import play.api.mvc._
import handlers.security._
import play.Logger
import secure._
import templates.TemplatesPlugin
import secure.providers.UsernamePasswordProvider
import utils.RoutesHelper
import play.api.Play
import play.api.Play.current
import scala.Some

/**
 * Created by wangrenhui on 14-2-20.
 */
object UserController extends Controller with Secure {

  /**
   * Login page.
   */
  def login = Action {
    implicit request =>
      if (Logger.isDebugEnabled()) {
        Logger.debug("current login")
      }
      val to = ProviderController.landingUrl
      if (Secure.currentUser.isDefined) {
        // if the user is already logged in just redirect to the app
        if (Logger.isDebugEnabled()) {
          Logger.debug("User already logged in, skipping login page. Redirecting to %s".format(to))
        }
        Redirect(to)
      } else {
        import com.typesafe.plugin._
        if (Secure.enableRefererAsOriginalUrl) {
          Secure.withRefererAsOriginalUrl(Ok(use[TemplatesPlugin].loginView(new MyDeadboltHandler, UsernamePasswordProvider.loginForm)))
        } else {
          Ok(use[TemplatesPlugin].loginView(new MyDeadboltHandler, UsernamePasswordProvider.loginForm))
        }
      }
  }

  /**
   * The property that specifies the page the user is redirected to after logging out.
   */
  val onLogoutGoTo = "onLogoutGoTo"

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    implicit request =>
      if (Logger.isDebugEnabled()) {
        Logger.debug("current logout")
      }
      val to = Play.configuration.getString(onLogoutGoTo).getOrElse(RoutesHelper.login().absoluteURL(UserProvider.sslEnabled))
      val user = for (
        authenticator <- Secure.authenticatorFromRequest;
        user <- UserService.find(authenticator.user.username)
      ) yield {
        Authenticator.delete(authenticator.id)
        user
      }
      val result = Redirect(to).discardingCookies(Authenticator.discardingCookie)
      user match {
        case Some(u) => result.withSession(Events.fire(new LogoutEvent(u)).getOrElse(session))
        case None => result
      }
  }
}
