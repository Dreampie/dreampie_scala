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
package secure.templates

import play.api.{Logger, Plugin, Application}
import play.api.data.Form
import be.objectify.deadbolt.scala.DeadboltHandler
import play.api.mvc.{AnyContent, Controller, RequestHeader, Request}
import play.api.templates.{Html, Txt}
import models.security._
import controllers.security.Registration.RegistrationInfo

/**
 * A trait that defines methods that return the html pages and emails for dreampie.
 *
 * If you need to customise the views just create a new plugin
 * and register it instead of DefaultTemplatesPlugin in the play.plugins file of your app.
 *
 * @see DefaultViewsPlugins
 */
trait TemplatesPlugin extends Plugin with Controller {
  override def onStart() {
    Logger.info("[dreampie] loaded templates plugin: %s".format(getClass.getName))
  }

  /**
   * Returns the html for the login page
   * @param request
   * @return
   */
  def loginView(deadboltHandler: DeadboltHandler, form: Form[(String, String, Boolean)], msg: Option[String] = None)(implicit request: Request[AnyContent]): Html

  /**
   * Returns the html for the signup page
   *
   * @param request
   * @return
   */
  def signUpView(form: Form[RegistrationInfo], token: String)(implicit request: Request[AnyContent]): Html

  /**
   * Returns the html for the start signup page
   *
   * @param request
   * @return
   */
  def startSignUpView(form: Form[String])(implicit request: Request[AnyContent]): Html

  /**
   * Returns the html for the reset password page
   *
   * @param request
   * @return
   */
  def resetPasswordView(form: Form[(String, String)], token: String)(implicit request: Request[AnyContent]): Html

  /**
   * Returns the html for the start reset page
   *
   * @param request
   * @return
   */
  def startResetPasswordView(form: Form[String])(implicit request: Request[AnyContent]): Html

  /**
   * Returns the html for the not authorized page
   *
   * @param request
   * @return
   */
  def notAuthorizedView(implicit request: Request[AnyContent]): Html

  /**
   * Returns the email sent when a user starts the sign up process
   *
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def signUpEmail(token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html])

  /**
   * Returns the email sent when the user is already registered
   *
   * @param user the user
   * @param request the current request
   * @return a tuple with the text and/or html body for the email
   */
  def alreadyRegisteredEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html])

  /**
   * Returns the welcome email sent when the user finished the sign up process
   *
   * @param user the user
   * @param request the current request
   * @return a String with the text and/or html body for the email
   */
  def welcomeEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html])

  /**
   * Returns the email sent when a user tries to reset the password but there is no account for
   * that email address in the system
   *
   * @param request the current request
   * @return a String with the text and/or html body for the email
   */
  def unknownEmailNotice()(implicit request: RequestHeader): (Option[Txt], Option[Html])

  /**
   * Returns the email sent to the user to reset the password
   *
   * @param user the user
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def sendPasswordResetEmail(user: User, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html])

  /**
   * Returns the email sent as a confirmation of a password change
   *
   * @param user the user
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def passwordChangedNoticeEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html])
}

/**
 * The default views plugin.  If you need to customise the views just create a new plugin that
 * extends TemplatesPlugin and register it in the play.plugins file instead of this one.
 *
 * @param application
 */
class DefaultTemplatesPlugin(application: Application) extends TemplatesPlugin {
  override def loginView(deadboltHandler: DeadboltHandler, form: Form[(String, String, Boolean)], msg: Option[String] = None)(implicit request: Request[AnyContent]): Html = {
    views.html.login(deadboltHandler, form, msg)
  }

  override def signUpView(form: Form[RegistrationInfo], token: String)(implicit request: Request[AnyContent]): Html = {
    views.html.Registration.signUp(form, token)
  }

  override def startSignUpView(form: Form[String])(implicit request: Request[AnyContent]): Html = {
    views.html.Registration.startSignUp(form)
  }

  override def resetPasswordView(form: Form[(String, String)], token: String)(implicit request: Request[AnyContent]): Html = {
    views.html.Registration.resetPassword(form, token)
  }


  override def startResetPasswordView(form: Form[String])(implicit request: Request[AnyContent]): Html = {
    views.html.Registration.startResetPassword(form)
  }


  def notAuthorizedView(implicit request: Request[AnyContent]): Html = {
    views.html.exceptions.notAuthorized()
  }

  def signUpEmail(token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.signUpEmail(token)))
  }

  def alreadyRegisteredEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.alreadyRegisteredEmail(user)))
  }

  def welcomeEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.welcomeEmail(user)))
  }

  def unknownEmailNotice()(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.unknownEmailNotice()))
  }

  def sendPasswordResetEmail(user: User, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.passwordResetEmail(user, token)))
  }

  def passwordChangedNoticeEmail(user: User)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.mails.passwordChangedNotice(user)))
  }
}