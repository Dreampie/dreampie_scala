package services

import models.security.{Token, User}
import utils.hasher.{BCryptPasswordHasher, PasswordHasher, Hashers}
import secure.UserServicePlugin
import scalikejdbc.SQLInterpolation._
import utils.hasher.PasswordInfo
import scala.Some
import org.joda.time.DateTime
import play.api.{Play, Logger, Application}

/**
 * Created by wangrenhui on 14-2-20.
 */
class DbUserService(application: Application) extends UserServicePlugin(application) {
  val TokenDurationKey = "userpass.remembermeDuration"
  val DefaultDuration = 7 * 24 * 60
  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)

  /**
   * 登录验证
   * @param username 用户名
   * @param password 密码
   * @return 用户对象
   */
  override def authenticate(username: String, password: String, rememberme: Boolean): Option[User] = {
    if (Logger.isDebugEnabled) {
      Logger.debug(s"login username and password as - ${username}:${password}")
    }
    val u: Option[User] = User.findByUsername(username)
    u match {
      case Some(u) =>
        if (Hashers.BCryptHasher.toString.equalsIgnoreCase(u.hasher)) {
          val ph: PasswordHasher = BCryptPasswordHasher
          if (ph.matches(PasswordInfo(u.hasher, u.password, u.salt), password)) {
            if (rememberme)
              TokenService.createToken(u.username, TokenDuration, false)
            Some(u)
          } else {
            None
          }
        } else {
          None
        }
      case _ => None
    }
  }

  /**
   * Finds a SocialUser that maches the specified name
   *
   * @param username the user name
   * @return an optional user
   */
  def find(username: String): Option[User] = User.findByUsername(username)

  /**
   * Finds a Social user by email and provider name.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation.
   *
   * @param username - the user email
   * @param providername - the provider name
   * @return
   */
  def findByNameAndProvider(username: String, providername: String): Option[User] = User.findByUserProvider(username, providername)

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param user
   */
  def save(user: User): User = User.create(user)

  /**
   * Links the current user User to another
   *
   * @param current The User of the current user
   * @param to The User that needs to be linked to the current user
   */
  def link(current: User, to: User): Unit = ???

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  def save(token: Token): Unit = Token.create(token)

  val tColumn = Token.column

  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token name
   * @return
   */
  def findToken(uuid: String): Option[Token] = Token.findBy(sqls.eq(tColumn.uuid, uuid))

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token name
   */
  def deleteToken(uuid: String): Unit = Token.destroyBy(sqls.eq(tColumn.uuid, uuid))

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens(): Unit = Token.destroyBy(sqls.lt(tColumn.expirationAt, DateTime.now))
}
