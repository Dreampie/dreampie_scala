package services

import models.security.Token
import org.joda.time.DateTime
import java.util.UUID
import secure.UserService

/**
 * Created by wangrenhui on 14-3-5.
 */
object TokenService {

  //  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)
  //  def rememberMe(username: String, isSignUp: Boolean): Token = {
  //    val now = DateTime.now
  //    val uuid = UUID.randomUUID().toString
  //    Token.create(uuid, username, now, now.plusSeconds(maxAge), Option(isSignUp))
  //  }

  def createToken(username: String, tokenDuration: Int, isSignUp: Boolean): (String, Token) = {
    val uuid = UUID.randomUUID().toString
    val now = DateTime.now

    val token = Token(
      uuid,
      username,
      now,
      now.plusMinutes(tokenDuration),
      isSignUp
    )
    UserService.save(token)
    (uuid, token)
  }
}
