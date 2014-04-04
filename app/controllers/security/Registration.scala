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

import secure.templates.TemplatesPlugin
import secure.providers.UsernamePasswordProvider
import secure._
import utils._
import scala.language.reflectiveCalls
import play.api._
import play.api.data._
import play.api.mvc.{RequestHeader, Result, Action, Controller}
import utils.validator.{RegValidator, PasswordValidator}
import services.TokenService
import utils.hasher.BCryptPasswordHasher
import com.typesafe.plugin._
import Play.current
import play.api.data.Forms._
import scala.Some
import secure.PasswordResetEvent
import play.api.i18n.Messages
import org.joda.time.DateTime
import models.security._

/**
 * A controller to handle user registration.
 *
 */
object Registration extends Controller {

  val Providername = UsernamePasswordProvider.UsernamePassword
  val EmailAlreadyTaken = "signup.emailAlreadyTaken"
  val EmailUniversity = "signup.emailUniversity"
  val UserNameAlreadyTaken = "signup.userNameAlreadyTaken"
  val PasswordsDoNotMatch = "signup.passwordsDoNotMatch"
  val ThankYouCheckEmail = "signup.thankYouCheckEmail"
  val InvalidLink = "signup.invalidLink"
  val SignUpDone = "signup.signUpDone"
  val PasswordUpdated = "password.passwordUpdated"
  val ErrorUpdatingPassword = "password.error"

  val Username = "username"
  val Email = "email"
  val FirstName = "firstName"
  val LastName = "lastName"
  val Password = "password"
  val Password1 = "password1"
  val Password2 = "password2"
  val Gender = "gender"
  val Success = "success"
  val Error = "error"

  val TokenDurationKey = "userpass.tokenDuration"
  val RegistrationEnabled = "registrationEnabled"
  val DefaultDuration = 60
  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)

  /** The redirect target of the handleStartSignUp action. */
  val onHandleStartSignUpGoTo = stringConfig("onStartSignUpGoTo", RoutesHelper.login().url)
  /** The redirect target of the handleSignUp action. */
  val onHandleSignUpGoTo = stringConfig("onSignUpGoTo", RoutesHelper.login().url)
  /** The redirect target of the handleStartResetPassword action. */
  val onHandleStartResetPasswordGoTo = stringConfig("onStartResetPasswordGoTo", RoutesHelper.login().url)
  /** The redirect target of the handleResetPassword action. */
  val onHandleResetPasswordGoTo = stringConfig("onResetPasswordGoTo", RoutesHelper.login().url)

  lazy val registrationEnabled = Play.current.configuration.getBoolean(RegistrationEnabled).getOrElse(true)

  private def stringConfig(key: String, default: => String) = {
    Play.current.configuration.getString(key).getOrElse(default)
  }

  case class RegistrationInfo(username: String, email: Option[String], firstName: String, lastName: String, password: String, gender: Int)


  val formWithUsername = Form[RegistrationInfo](
    mapping(
      Username -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        UserService.findByNameAndProvider(userName, Providername).isEmpty
      }),
      Email -> email.verifying(
        Messages(EmailAlreadyTaken), email => User.findByUsername(email).isEmpty).verifying(
          Messages(EmailUniversity), email => RegValidator.isEmail(email)),
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText
        ).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2),
      Gender -> number
    )
      // binding
      ((username, email, firstName, lastName, password, gender) => RegistrationInfo(username, Some(email), firstName, lastName, password._1, gender))
      // unbinding
      (info => Some(info.username, info.email.getOrElse(""), info.firstName, info.lastName, ("", ""), info.gender))
  )


  val form = formWithUsername

  val userForm = Form(
    Username -> nonEmptyText
  )

  /**
   * Starts the sign up process
   */
  def startSignUp = Action {
    implicit request =>
      if (registrationEnabled) {
        if (Secure.enableRefererAsOriginalUrl) {
          Secure.withRefererAsOriginalUrl(Ok(use[TemplatesPlugin].startSignUpView(userForm)))
        } else {
          Ok(use[TemplatesPlugin].startSignUpView(userForm))
        }
      }
      else NotFound(views.html.defaultpages.notFound.render(request, None))
  }

  def handleStartSignUp = Action {
    implicit request =>
      if (registrationEnabled) {
        startForm.bindFromRequest.fold(
          errors => {
            BadRequest(use[TemplatesPlugin].startSignUpView(errors))
          },
          email => {
            // check if there is already an account for this email address
            UserService.findByNameAndProvider(email, UsernamePasswordProvider.UsernamePassword) match {
              case Some(user) => {
                // user signed up already, send an email offering to login/recover password
                Mailer.sendAlreadyRegisteredEmail(user)
              }
              case None => {
                val token = TokenService.createToken(email, TokenDuration, false)
                Mailer.sendSignUpEmail(email, token._1)
              }
            }
            Redirect(onHandleStartSignUpGoTo).flashing(Success -> Messages(ThankYouCheckEmail), Email -> email)
          }
        )
      }
      else NotFound(views.html.defaultpages.notFound.render(request, None))
  }

  /**
   * Renders the sign up page
   * @return
   */
  def signUp(token: String) = Action {
    implicit request =>
      if (registrationEnabled) {
        if (Logger.isDebugEnabled) {
          Logger.debug("[dreampie] trying sign up with token %s".format(token))
        }
        executeForToken(token, true, {
          _ =>
            Ok(use[TemplatesPlugin].signUpView(form, token))
        })
      }
      else NotFound(views.html.defaultpages.notFound.render(request, None))
  }

  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = Action {
    implicit request =>
      if (registrationEnabled) {
        executeForToken(token, true, {
          t =>
            form.bindFromRequest.fold(
              errors => {
                if (Logger.isDebugEnabled) {
                  Logger.debug("[dreampie] errors " + errors)
                }
                BadRequest(use[TemplatesPlugin].signUpView(errors, t.uuid))
              },
              info => {
                val passinfo = BCryptPasswordHasher.hash(info.password)
                val user: User = new User(
                  id = 0,
                  username = info.username,
                  providername = Providername,
                  email = info.email,
                  mobile = None,
                  password = passinfo.password,
                  hasher = passinfo.hasher,
                  salt = passinfo.salt,
                  avatarUrl = None,
                  firstName = info.firstName,
                  lastName = info.lastName,
                  fullName = "%s %s".format(info.firstName, info.lastName),
                  createdAt = DateTime.now,
                  updatedAt = None,
                  deletedAt = None,
                  userInfo = None,
                  roles = Nil,
                  permissions = Nil,
                  authMethod = AuthenticationMethod.UserPassword
                )
                val saved = UserService.save(user)
                UserService.deleteToken(t.uuid)
                if (UsernamePasswordProvider.sendWelcomeEmail) {
                  Mailer.sendWelcomeEmail(saved)
                }
                val eventSession = Events.fire(new SignUpEvent(user)).getOrElse(session)
                if (UsernamePasswordProvider.signupSkipLogin) {
                  ProviderController.completeAuthentication(user, eventSession).flashing(Success -> Messages(SignUpDone))
                } else {
                  Redirect(onHandleSignUpGoTo).flashing(Success -> Messages(SignUpDone)).withSession(eventSession)
                }
              }
            )
        })
      }
      else NotFound(views.html.defaultpages.notFound.render(request, None))
  }

  val startForm = Form(
    Email -> email.verifying(
      "Email already in use.", email => User.findByUsername(email).isEmpty).verifying(
        "University email required", email => RegValidator.isEmail(email))
  )
  val changePasswordForm = Form(
    Password ->
      tuple(
        Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
        Password2 -> nonEmptyText
      ).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
  )


  def startResetPassword = Action {
    implicit request =>
      Ok(use[TemplatesPlugin].startResetPasswordView(startForm))
  }

  def handleStartResetPassword = Action {
    implicit request =>
      startForm.bindFromRequest.fold(
        errors => {
          BadRequest(use[TemplatesPlugin].startResetPasswordView(errors))
        },
        email => {
          UserService.findByNameAndProvider(email, UsernamePasswordProvider.UsernamePassword) match {
            case Some(user) => {
              val token = TokenService.createToken(email, TokenDuration, false)
              Mailer.sendPasswordResetEmail(user, token._1)
            }
            case None => {
              Mailer.sendUnkownEmailNotice(email)
            }
          }
          Redirect(onHandleStartResetPasswordGoTo).flashing(Success -> Messages(ThankYouCheckEmail))
        }
      )
  }

  def executeForToken(token: String, isSignUp: Boolean, f: Token => Result)(implicit request: RequestHeader): Result = {
    UserService.findToken(token) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ => {
        val to = if (isSignUp) RoutesHelper.startSignUp() else RoutesHelper.startResetPassword()
        Redirect(to).flashing(Error -> Messages(InvalidLink))
      }
    }
  }

  def resetPassword(token: String) = Action {
    implicit request =>
      executeForToken(token, false, {
        t =>
          Ok(use[TemplatesPlugin].resetPasswordView(changePasswordForm, token))
      })
  }

  def handleResetPassword(token: String) = Action {
    implicit request =>
      executeForToken(token, false, {
        t =>
          changePasswordForm.bindFromRequest.fold(errors => {
            BadRequest(use[TemplatesPlugin].resetPasswordView(errors, token))
          },
            p => {
              val (toFlash, eventSession) = UserService.findByNameAndProvider(t.username, UsernamePasswordProvider.UsernamePassword) match {
                case Some(user) => {
                  val pass = BCryptPasswordHasher.hash(p._1)
                  val updated = UserService.save(user.copy(hasher = pass.hasher, password = pass.password, salt = pass.salt))
                  UserService.deleteToken(token)
                  Mailer.sendPasswordChangedNotice(updated)
                  val eventSession = Events.fire(new PasswordResetEvent(updated))
                  (Success -> Messages(PasswordUpdated), eventSession)
                }
                case _ => {
                  Logger.error("[dreampie] could not find user with username %s during password reset".format(t.username))
                  (Error -> Messages(ErrorUpdatingPassword), None)
                }
              }
              val result = Redirect(onHandleResetPasswordGoTo).flashing(toFlash)
              eventSession.map(result.withSession).getOrElse(result)
            })
      })
  }
}
