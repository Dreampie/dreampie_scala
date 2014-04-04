/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package controllers.security

import play.api.mvc._
import play.api.i18n.Messages
import play.api.{Play, Logger}
import Play.current
import play.api.http.HeaderNames
import models.security._
import utils.RoutesHelper
import secure._
import exceptions.AccessDeniedException
import scala.Some
import play.api.mvc.SimpleResult
import secure.LoginEvent
import secure.providers.UsernamePasswordProvider

/**
 * A controller to provide the authentication entry point
 */
object ProviderController extends Controller with Secure {
  /**
   * The property that specifies the page the user is redirected to if there is no original URL saved in
   * the session.
   */
  val onLoginGoTo = "onLoginGoTo"

  /**
   * The root path
   */
  val Root = "/"

  /**
   * The application context
   */
  val ApplicationContext = "application.context"

  /**
   * Returns the url that the user should be redirected to after login
   *
   * @param session
   * @return
   */
  def toUrl(session: Session) = session.get(Secure.OriginalUrlKey).getOrElse(landingUrl)

  /**
   * The url where the user needs to be redirected after succesful authentication.
   *
   * @return
   */
  def landingUrl = Play.configuration.getString(onLoginGoTo).getOrElse(
    Play.configuration.getString(ApplicationContext).getOrElse(Root)
  )

  /**
   * Renders a not authorized page if the Authorization object passed to the action does not allow
   * execution.
   *
   * @see Authorization
   */
  def notAuthorized() = Action {
    implicit request =>
      Forbidden(views.html.exceptions.notAuthorized())
  }

  /**
   * The authentication flow for all providers starts here.
   *
   * @param provider The name of the provider that needs to handle the call
   * @return
   */
  def authenticate(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  def authenticateByPost(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  private def overrideOriginalUrl(session: Session, redirectTo: Option[String]) = redirectTo match {
    case Some(url) =>
      session + (Secure.OriginalUrlKey -> url)
    case _ =>
      session
  }

  private def handleAuth(provider: String, redirectTo: Option[String]) = UserAwareAction {
    implicit request =>
    //没有验证
//      val authenticationFlow = request.user.isEmpty
      val modifiedSession = overrideOriginalUrl(session, redirectTo)

      Registry.providers.get(provider) match {
        case Some(p) => {
          try {
            p.authenticate().fold(result => {
              redirectTo match {
                case Some(url) =>
                  val cookies = Cookies(result.header.headers.get(HeaderNames.SET_COOKIE))
                  val resultSession = Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))
                  result.withSession(resultSession + (Secure.OriginalUrlKey -> url))
                case _ => result
              }
            }, {
              user => //if (authenticationFlow) {
                if (!provider.equals(UsernamePasswordProvider.UsernamePassword)) {
                  UserService.findByNameAndProvider(user.username, user.providername) match {
                    case Some(u) => completeAuthentication(u, modifiedSession)
                    case _ => completeAuthentication(UserService.save(user), modifiedSession)
                  }
                } else
                  completeAuthentication(user, modifiedSession)
//              } else {
//                request.user match {
//                  case Some(currentUser) =>
//                    UserService.link(currentUser, user)
//                    if (Logger.isDebugEnabled) {
//                      Logger.debug(s"[dreampie] linked $currentUser to $user")
//                    }
//                    // improve this, I'm duplicating part of the code in completeAuthentication
//                    Redirect(toUrl(modifiedSession)).withSession(modifiedSession -
//                      Secure.OriginalUrlKey -
//                      UserProvider.SessionId)
//                  case _ =>
//                    Unauthorized
//                }
//              }
            })
          } catch {
            case ex: AccessDeniedException => {
              Redirect(RoutesHelper.login()).flashing("error" -> Messages("login.accessDenied"))
            }

            case other: Throwable => {
              Logger.error("Unable to log user in. An exception was thrown", other)
              Redirect(RoutesHelper.login()).flashing("error" -> Messages("login.errorLoggingIn"))
            }
          }
        }
        case _ => NotFound
      }
  }

  def completeAuthentication(user: User, session: Session)(implicit request: RequestHeader): SimpleResult = {
    if (Logger.isDebugEnabled) {
      Logger.debug("[dreampie] user logged in : [" + user + "]")
    }
    val withSession = Events.fire(new LoginEvent(user)).getOrElse(session)
    Authenticator.create(user) match {
      case Right(authenticator) => {
        Redirect(toUrl(withSession)).withSession(withSession -
          Secure.OriginalUrlKey -
          UserProvider.SessionId).withCookies(authenticator.toCookie)
      }
      case Left(error) => {
        // improve this
        throw new RuntimeException("Error creating authenticator")
      }
    }
  }
}
