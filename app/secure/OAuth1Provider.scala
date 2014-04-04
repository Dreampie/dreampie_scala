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

import _root_.java.util.UUID
import play.api.cache.Cache
import play.api.{Application, Logger, Play}
import play.api.mvc.{AnyContent, Request}
import play.api.mvc.Results.Redirect
import Play.current
import models.security._
import utils.RoutesHelper
import play.api.libs.oauth.OAuth
import exceptions.AccessDeniedException
import scala.Some
import play.api.mvc.SimpleResult
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey
import exceptions.AuthenticationException
import org.joda.time.DateTime

/**
 * Base class for all OAuth1 providers
 */
abstract class OAuth1Provider(application: Application) extends UserProvider(application) {
  val serviceInfo = createServiceInfo(propertyKey)
  val service = OAuth(serviceInfo, use10a = true)

  def authMethod = AuthenticationMethod.OAuth1

  def createServiceInfo(key: String): ServiceInfo = {
    val result = for {
      requestTokenUrl <- loadProperty(OAuth1Provider.RequestTokenUrl);
      accessTokenUrl <- loadProperty(OAuth1Provider.AccessTokenUrl);
      authorizationUrl <- loadProperty(OAuth1Provider.AuthorizationUrl);
      consumerKey <- loadProperty(OAuth1Provider.ConsumerKey);
      consumerSecret <- loadProperty(OAuth1Provider.ConsumerSecret)
    } yield {
      ServiceInfo(requestTokenUrl, accessTokenUrl, authorizationUrl, ConsumerKey(consumerKey, consumerSecret))
    }

    if (result.isEmpty) {
      throwMissingPropertiesException()
    }
    result.get
  }


  def doAuth()(implicit request: Request[AnyContent]): Either[SimpleResult, User] = {
    if (request.queryString.get("denied").isDefined) {
      // the user did not grant access to the account
      throw new AccessDeniedException()
    }

    request.queryString.get("oauth_verifier").map {
      seq =>
        val verifier = seq.head
        // 2nd step in the oauth flow, we have the access token in the cache, we need to
        // swap it for the access token
        val user = for {
          cacheKey <- request.session.get(OAuth1Provider.CacheKey)
          requestToken <- Cache.getAs[RequestToken](cacheKey)
        } yield {
          service.retrieveAccessToken(RequestToken(requestToken.token, requestToken.secret), verifier) match {
            case Right(token) =>
              Cache.remove(cacheKey)
              Right(
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
                  oAuth1Info = Some(OAuth1Info(token.token, token.secret)),
                  oAuth2Info = None)
              )
            case Left(oauthException) =>
              Logger.error("[dreampie] error retrieving access token", oauthException)
              throw new AuthenticationException()
          }
        }
        user.getOrElse(throw new AuthenticationException())
    }.getOrElse {
      // the oauth_verifier field is not in the request, this is the 1st step in the auth flow.
      // we need to get the request tokens
      val callbackUrl = RoutesHelper.authenticate(name).absoluteURL(UserProvider.sslEnabled)
      if (Logger.isDebugEnabled) {
        Logger.debug("[dreampie] callback url = " + callbackUrl)
      }
      service.retrieveRequestToken(callbackUrl) match {
        case Right(accessToken) =>
          val cacheKey = UUID.randomUUID().toString
          val redirect = Redirect(service.redirectUrl(accessToken.token)).withSession(request.session +
            (OAuth1Provider.CacheKey -> cacheKey))
          Cache.set(cacheKey, accessToken, 300) // set it for 5 minutes, plenty of time to log in
          Left(redirect)
        case Left(e) =>
          Logger.error("[dreampie] error retrieving request token", e)
          throw new AuthenticationException()
      }
    }
  }

}

object OAuth1Provider {
  val CacheKey = "cacheKey"
  val RequestTokenUrl = "requestTokenUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrl = "authorizationUrl"
  val ConsumerKey = "consumerKey"
  val ConsumerSecret = "consumerSecret"
}
