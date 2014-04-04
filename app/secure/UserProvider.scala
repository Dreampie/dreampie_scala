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
package secure

import utils.RoutesHelper
import play.api.mvc.{SimpleResult, AnyContent, Request}
import play.api.{Play, Application, Logger, Plugin}
import concurrent.{Await, Future}
import play.api.libs.ws.Response
import models.security._

/**
 * Base class for all User Providers.  All providers are plugins and are loaded
 * automatically at application start time.
 *
 *
 */
abstract class UserProvider(application: Application) extends Plugin with Registrable {
  val providerKey = "providers."
  val Dot = "."


  /**
   * Registers the provider in the Provider Registry
   */
  override def onStart() {
    Logger.info("[dreampie] loaded User provider: %s".format(name))
    Registry.providers.register(this)
  }

  /**
   * Unregisters the provider
   */
  override def onStop() {
    Logger.info("[dreampie] unloaded User provider: %s".format(name))
    Registry.providers.unRegister(name)
  }

  /**
   * Subclasses need to implement this to specify the authentication method
   * @return
   */
  def authMethod: AuthenticationMethod

  /**
   * Returns the provider name
   *
   * @return
   */
  override def toString = name

  /**
   * Authenticates the user and fills the profile information. Returns either a User if all went
   * ok or a Result that the controller sends to the browser (eg: in the case of OAuth for example
   * where the user needs to be redirected to the service provider)
   *
   * @param request
   * @return
   */
  def authenticate()(implicit request: Request[AnyContent]): Either[SimpleResult, User] = {
    doAuth().fold(
      result => Left(result),
      u => Right(fillProfile(u))
    )
  }

  /**
   * The url for this provider. This is used in the login page template to point each icon
   * to the provider url.
   * @return
   */
  def authenticationUrl: String = RoutesHelper.authenticate(name).url

  def authenticationUrl(redirectTo: String): String = RoutesHelper.authenticate(name, Some(redirectTo)).url

  /**
   * The property key used for all the provider properties.
   *
   * @return
   */
  def propertyKey = providerKey + name + Dot

  /**
   * Reads a property from the application.conf
   * @param property
   * @return
   */
  def loadProperty(property: String): Option[String] = {
    val result = application.configuration.getString(propertyKey + property)
    if (!result.isDefined) {
      Logger.error("[dreampie] Missing property " + property + " for provider " + name)
    }
    result
  }


  /**
   * Subclasses need to implement the authentication logic. This method needs to return
   * a User object that then gets passed to the fillProfile method
   *
   * @param request
   * @return Either a Result or a User
   */
  def doAuth()(implicit request: Request[AnyContent]): Either[SimpleResult, User]

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: User): User

  protected def throwMissingPropertiesException() {
    val msg = "[dreampie] Missing properties for provider '%s'. Verify your configuration file is properly set.".format(name)
    Logger.error(msg)
    throw new RuntimeException(msg)
  }

  protected def awaitResult(future: Future[Response]) = {
    Await.result(future, UserProvider.secondsToWait)
  }
}

object UserProvider {
  val SessionId = "sid"

  val sslEnabled: Boolean = {
    import Play.current
    val result = current.configuration.getBoolean("ssl").getOrElse(false)
    if (!result && Play.isProd) {
      Logger.warn(
        "[dreampie] IMPORTANT: Play is running in production mode but you did not turn SSL on for dreampie." +
          "Not using SSL can make it really easy for an attacker to steal your users' credentials and/or the " +
          "authenticator cookie and gain access to the system."
      )
    }
    result
  }

  val secondsToWait = {
    import scala.concurrent.duration._
    10.seconds
  }
}
