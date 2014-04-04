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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.{Play, Application}
import Play.current
import scala.Some
import play.api.mvc.SimpleResult
import models.security._
import be.objectify.deadbolt.scala.DeadboltHandler
import secure.templates.TemplatesPlugin
import com.typesafe.plugin._
import handlers.security.MyDeadboltHandler
import secure.{UserService, AuthenticationMethod, UserProvider}
import utils.GravatarHelper

/**
 * A username password provider
 */
class UsernamePasswordProvider(application: Application) extends UserProvider(application) {

  override def name = UsernamePasswordProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "login.invalidCredentials"

  def doAuth()(implicit request: Request[AnyContent]): Either[SimpleResult, User] = {
    val form = UsernamePasswordProvider.loginForm.bindFromRequest()
    form.fold(
      formWithErrors => Left(badRequest(new MyDeadboltHandler, formWithErrors)(request)),
      credentials => {
        val result = for (
          user <- UserService.authenticate(credentials._1, credentials._2, credentials._3)
        ) yield Right(user)
        result.getOrElse(
          Left(badRequest(new MyDeadboltHandler, UsernamePasswordProvider.loginForm, Some(InvalidCredentials)))
        )
      }
    )
  }

  private def badRequest[A](deadboltHandler: DeadboltHandler, form: Form[(String, String, Boolean)], msg: Option[String] = None)(implicit request: Request[AnyContent]): SimpleResult = {
    Results.BadRequest(use[TemplatesPlugin].loginView(deadboltHandler, form, msg))
  }

  def fillProfile(user: User) = {
    user.email match {
      case Some(user.email) => GravatarHelper.avatarFor(user.email.get) match {
        case Some(url) if url != user.avatarUrl => user.copy(avatarUrl = Some(url))
        case _ => user
      }
      case _ => user
    }

  }
}

object UsernamePasswordProvider {
  val UsernamePassword = "userpass"
  private val SendWelcomeEmailKey = "userpass.sendWelcomeEmail"
  private val EnableGravatarKey = "userpass.enableGravatarSupport"
  private val EnableTokenJob = "userpass.enableTokenJob"
  private val SignupSkipLogin = "userpass.signupSkipLogin"

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text,
      "rememberme" -> boolean
    )
  )

  lazy val sendWelcomeEmail = current.configuration.getBoolean(SendWelcomeEmailKey).getOrElse(true)
  lazy val enableTokenJob = current.configuration.getBoolean(EnableTokenJob).getOrElse(true)
  lazy val signupSkipLogin = current.configuration.getBoolean(SignupSkipLogin).getOrElse(false)
  lazy val enableGravatar = current.configuration.getBoolean(EnableGravatarKey).getOrElse(true)
}

