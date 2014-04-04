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

import _root_.java.net.URLEncoder
import _root_.java.util.UUID
import play.api.{Logger, Play, Application}
import play.api.cache.Cache
import Play.current
import play.api.mvc._
import utils.RoutesHelper
import play.api.libs.ws.WS
import scala.collection.JavaConversions._
import models.security._
import exceptions.AccessDeniedException
import play.api.libs.ws.Response
import scala.Some
import play.api.mvc.SimpleResult
import exceptions.AuthenticationException
import org.joda.time.DateTime

/**
 * Base class for all OAuth2 providers
 */
abstract class OAuth2Provider(application: Application, jsonResponse: Boolean = true) extends UserProvider(application) {
  val settings = createSettings()

  def authMethod = AuthenticationMethod.OAuth2

  private def createSettings(): OAuth2Settings = {
    val result = for {
      authorizationUrl <- loadProperty(OAuth2Settings.AuthorizationUrl);
      accessToken <- loadProperty(OAuth2Settings.AccessTokenUrl);
      clientId <- loadProperty(OAuth2Settings.ClientId);
      clientSecret <- loadProperty(OAuth2Settings.ClientSecret)
    } yield {
      val scope = application.configuration.getString(propertyKey + OAuth2Settings.Scope)
      val authorizationUrlParams: Map[String, String] =
        application.configuration.getObject(propertyKey + OAuth2Settings.AuthorizationUrlParams).map {
          o =>
            o.unwrapped.toMap.mapValues(_.toString)
        }.getOrElse(Map())
      val accessTokenUrlParams: Map[String, String] =
        application.configuration.getObject(propertyKey + OAuth2Settings.AccessTokenUrlParams).map {
          o =>
            o.unwrapped.toMap.mapValues(_.toString)
        }.getOrElse(Map())
      OAuth2Settings(authorizationUrl, accessToken, clientId, clientSecret, scope, authorizationUrlParams, accessTokenUrlParams)
    }
    if (!result.isDefined) {
      throwMissingPropertiesException()
    }
    result.get
  }

  private def getAccessToken[A](code: String)(implicit request: Request[A]): OAuth2Info = {
    val params = Map(
      OAuth2Constants.ClientId -> Seq(settings.clientId),
      OAuth2Constants.ClientSecret -> Seq(settings.clientSecret),
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(RoutesHelper.authenticate(name).absoluteURL(UserProvider.sslEnabled))
    ) ++ settings.accessTokenUrlParams.mapValues(Seq(_))
    val call = WS.url(settings.accessTokenUrl).post(params)
    try {
      buildInfo(awaitResult(call))
    } catch {
      case e: Exception => {
        Logger.error("[dreampie] error trying to get an access token for provider %s".format(name), e)
        throw new AuthenticationException()
      }
    }
  }

  protected def buildInfo(response: Response): OAuth2Info = {
    val json = response.json
    if (Logger.isDebugEnabled) {
      Logger.debug("[dreampie] got json back [" + json + "]")
    }
    OAuth2Info(
      (json \ OAuth2Constants.AccessToken).as[String],
      (json \ OAuth2Constants.TokenType).asOpt[String],
      (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
      (json \ OAuth2Constants.RefreshToken).asOpt[String]
    )
  }

  def doAuth()(implicit request: Request[AnyContent]): Either[SimpleResult, User] = {
    request.queryString.get(OAuth2Constants.Error).flatMap(_.headOption).map(error => {
      error match {
        case OAuth2Constants.AccessDenied => throw new AccessDeniedException()
        case _ =>
          Logger.error("[dreampie] error '%s' returned by the authorization server. Provider type is %s".format(error, name))
          throw new AuthenticationException()
      }
      throw new AuthenticationException()
    })

    request.queryString.get(OAuth2Constants.Code).flatMap(_.headOption) match {
      case Some(code) =>
        // we're being redirected back from the authorization server with the access code.
        val user = for (
        // check if the state we sent is equal to the one we're receiving now before continuing the flow.
          sessionId <- request.session.get(UserProvider.SessionId);
          // todo: review this -> clustered environments
          originalState <- Cache.getAs[String](sessionId);
          currentState <- request.queryString.get(OAuth2Constants.State).flatMap(_.headOption) if originalState == currentState
        ) yield {
          val accessToken = getAccessToken(code)
          val oauth2Info = Some(
            OAuth2Info(accessToken.accessToken, accessToken.tokenType, accessToken.expiresIn, accessToken.refreshToken)
          )
          User(
            id = 0,
            username = "",
            providername = name,
            email = None,
            mobile = None,
            hasher = "",
            password = "",
            salt = "",
            avatarUrl = None,
            firstName = "",
            lastName = "",
            fullName = "",
            createdAt = DateTime.now,
            updatedAt = None,
            deletedAt = None,
            roles = Nil,
            permissions = Nil,
            authMethod = authMethod,
            oAuth1Info = None,
            oAuth2Info = oauth2Info)
        }

        if (Logger.isDebugEnabled) {
          Logger.debug("[dreampie] user = %s".format(user))
        }
        user match {
          case Some(u) => Right(u)
          case _ => throw new AuthenticationException()
        }
      case None =>
        // There's no code in the request, this is the first step in the oauth flow
        val state = UUID.randomUUID().toString
        val sessionId = request.session.get(UserProvider.SessionId).getOrElse(UUID.randomUUID().toString)
        Cache.set(sessionId, state, 300)
        var params = List(
          (OAuth2Constants.ClientId, settings.clientId),
          (OAuth2Constants.RedirectUri, RoutesHelper.authenticate(name).absoluteURL(UserProvider.sslEnabled)),
          (OAuth2Constants.ResponseType, OAuth2Constants.Code),
          (OAuth2Constants.State, state))
        settings.scope.foreach(s => {
          params = (OAuth2Constants.Scope, s) :: params
        })
        settings.authorizationUrlParams.foreach(e => {
          params = e :: params
        })
        val url = settings.authorizationUrl +
          params.map(p => URLEncoder.encode(p._1, "UTF-8") + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
        if (Logger.isDebugEnabled) {
          Logger.debug("[dreampie] authorizationUrl = %s".format(settings.authorizationUrl))
          Logger.debug("[dreampie] redirecting to: [%s]".format(url))
        }
        Left(Results.Redirect(url).withSession(request.session +(UserProvider.SessionId, sessionId)))
    }
  }

}

case class OAuth2Settings(authorizationUrl: String, accessTokenUrl: String, clientId: String,
                          clientSecret: String, scope: Option[String],
                          authorizationUrlParams: Map[String, String], accessTokenUrlParams: Map[String, String]
                           )

object OAuth2Settings {
  val AuthorizationUrl = "authorizationUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrlParams = "authorizationUrlParams"
  val AccessTokenUrlParams = "accessTokenUrlParams"
  val ClientId = "clientId"
  val ClientSecret = "clientSecret"
  val Scope = "scope"
}

object OAuth2Constants {
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val RedirectUri = "redirect_uri"
  val Scope = "scope"
  val ResponseType = "response_type"
  val State = "state"
  val GrantType = "grant_type"
  val AuthorizationCode = "authorization_code"
  val AccessToken = "access_token"
  val Error = "error"
  val Code = "code"
  val TokenType = "token_type"
  val ExpiresIn = "expires_in"
  val RefreshToken = "refresh_token"
  val AccessDenied = "access_denied"
}
