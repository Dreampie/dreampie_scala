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
package secure.providers

import play.api.{Logger, Application}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import scala.Some
import secure._
import exceptions._
import models.security._

/**
 * A GitHub provider
 *
 */
class GitHubProvider(application: Application) extends OAuth2Provider(application) {
  val GetAuthenticatedUser = "https://api.github.com/user?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Message = "message"
  val Id = "id"
  val Name = "name"
  val AvatarUrl = "avatar_url"
  val Email = "email"

  override def name = GitHubProvider.GitHub

  override protected def buildInfo(response: Response): OAuth2Info = {
    Logger.info("[secure.providers.GitHubProvider] response body - %s".format(response.body))
    val values: Map[String, String] = response.body.split("&").map(_.split("="))
      .withFilter(_.size == 2).map(r => (r(0), r(1)))(collection.breakOut)
    val accessToken = values.get(OAuth2Constants.AccessToken)
    if (accessToken.isEmpty) {
      Logger.error("[secure.providers.GitHubProvider] did not get accessToken from %s".format(name))
      throw new AuthenticationException()
    }
    OAuth2Info(
      accessToken.get,
      values.get(OAuth2Constants.TokenType),
      values.get(OAuth2Constants.ExpiresIn).map(_.toInt),
      values.get(OAuth2Constants.RefreshToken)
    )
  }

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: User): User = {
    val promise = WS.url(GetAuthenticatedUser.format(user.oAuth2Info.get.accessToken)).get()
    try {
      val response = awaitResult(promise)
      val me = response.json
      (me \ Message).asOpt[String] match {
        case Some(msg) => {
          Logger.error("[secure.providers.GitHubProvider] error retrieving profile information from GitHub. Message = %s".format(msg))
          throw new AuthenticationException()
        }
        case _ => {
          Logger.info("[secure.providers.GitHubProvider] response.json - %s".format(me))
          val username = (me \ Id).as[Int]
          val displayName = (me \ Name).asOpt[String].getOrElse("")
          val avatarUrl = (me \ AvatarUrl).asOpt[String]
          val email = (me \ Email).asOpt[String].filter(!_.isEmpty)

          Logger.info("[secure.providers.GitHubProvider] username = %s,providername=%s,fullName=%s,avatarUrl=%s,email=%s".format(username, name, displayName, avatarUrl, email))
          user.copy(
            username = username.toString,
            providername = name,
            fullName = displayName,
            avatarUrl = avatarUrl,
            email = email
          )
        }
      }
    } catch {
      case e: Exception => {
        Logger.error("[secure.providers.GitHubProvider] error retrieving profile information from github", e)
        throw new AuthenticationException()
      }
    }
  }
}

object GitHubProvider {
  val GitHub = "github"
}