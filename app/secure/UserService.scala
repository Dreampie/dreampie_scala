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

import play.api.{Logger, Plugin, Application}
import providers.UsernamePasswordProvider
import play.api.libs.concurrent.Akka
import akka.actor.Cancellable
import models.security._

/**
 * A trait that provides the means to find and save users
 * for the dreampie module.
 *
 * @see DefaultUserService
 */
trait UserService {

  /**
   * 密码认证
   * @param username
   * @param password
   * @return
   */
  def authenticate(username: String, password: String, rememberme: Boolean): Option[User]

  /**
   * Finds a User that maches the specified name
   *
   * @param username the user name
   * @return an optional user
   */
  def find(username: String): Option[User]

  /**
   * Finds a Social user by email and provider name.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation.
   *
   * @param username - the user username
   * @param providername - the provider name
   * @return
   */
  def findByNameAndProvider(username: String, providername: String): Option[User]

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param user
   */
  def save(user: User): User

  /**
   * Links the current user User to another
   *
   * @param current The User of the current user
   * @param to The User that needs to be linked to the current user
   */
  def link(current: User, to: User)

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  def save(token: Token)


  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token name
   * @return
   */
  def findToken(uuid: String): Option[Token]

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token name
   */
  def deleteToken(uuid: String)

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens()
}

/**
 * Base class for the classes that implement UserService.  Since this is a plugin it gets loaded
 * at application start time.  Only one plugin of this type must be specified in the play.plugins file.
 *
 * @param application
 */
abstract class   UserServicePlugin(application: Application) extends Plugin with UserService {
  val DefaultInterval = 5
  val DeleteIntervalKey = "userpass.tokenDeleteInterval"

  var cancellable: Option[Cancellable] = None

  override def onStop() {
    cancellable.map(_.cancel())
  }

  /**
   * Registers this object so dreampie can invoke it.
   */
  override def onStart() {
    import play.api.Play.current
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Execution.Implicits._
    val i = application.configuration.getInt(DeleteIntervalKey).getOrElse(DefaultInterval)

    cancellable = if (UsernamePasswordProvider.enableTokenJob) {
      Some(
        Akka.system.scheduler.schedule(0.seconds, i.minutes) {
          if (Logger.isDebugEnabled) {
            Logger.debug("[dreampie] calling deleteExpiredTokens()")
          }
          deleteExpiredTokens()
        }
      )
    } else None

    UserService.setService(this)
    Logger.info("[dreampie] loaded user service: %s".format(this.getClass))
  }
}

/**
 * The UserService singleton
 */
object UserService {
  var delegate: Option[UserService] = None

  def setService(service: UserService) {
    delegate = Some(service)
  }

  def authenticate(username: String, password: String, rememberme: Boolean): Option[User] = {
    delegate.map(_.authenticate(username, password, rememberme)).getOrElse {
      notInitialized()
      None
    }
  }

  def find(username: String): Option[User] = {
    delegate.map(_.find(username)).getOrElse {
      notInitialized()
      None
    }
  }

  def findByNameAndProvider(username: String, providername: String): Option[User] = {
    delegate.map(_.findByNameAndProvider(username, providername)).getOrElse {
      notInitialized()
      None
    }
  }

  def save(user: User): User = {
    delegate.map(_.save(user)).getOrElse {
      notInitialized()
      user
    }
  }

  def link(current: User, to: User) {
    delegate.map(_.link(current, to)).getOrElse {
      notInitialized()
    }
  }

  def save(token: Token) {
    delegate.map(_.save(token)).getOrElse {
      notInitialized()
    }
  }

  def findToken(token: String): Option[Token] = {
    delegate.map(_.findToken(token)).getOrElse {
      notInitialized()
      None
    }
  }

  def deleteToken(token: String) {
    delegate.map(_.deleteToken(token)).getOrElse {
      notInitialized()
    }
  }


  private def notInitialized() {
    Logger.error("[dreampie] UserService was not initialized. Make sure a UserService plugin is specified in your play.plugins file")
    throw new RuntimeException("UserService not initialized")
  }
}
