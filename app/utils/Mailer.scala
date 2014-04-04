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
package utils

import play.api.{Play, Logger}
import Play.current
import play.api.libs.concurrent.Akka
import play.api.templates.{Html, Txt}
import utils.validator.RegValidator
import exceptions.ValidateException
import secure.templates.TemplatesPlugin
import models.security._
import play.api.mvc.RequestHeader
import play.api.i18n.{Lang, Messages}
import com.typesafe.plugin._

/**
 * A helper class to send email notifications
 */
object Mailer {
  val fromAddress = current.configuration.getString("smtp.from").get
  val AlreadyRegisteredSubject = "mails.sendAlreadyRegisteredEmail.subject"
  val SignUpEmailSubject = "mails.sendSignUpEmail.subject"
  val WelcomeEmailSubject = "mails.welcomeEmail.subject"
  val PasswordResetSubject = "mails.passwordResetEmail.subject"
  val UnknownEmailNoticeSubject = "mails.unknownEmail.subject"
  val PasswordResetOkSubject = "mails.passwordResetOk.subject"


  def sendAlreadyRegisteredEmail(user: User)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].alreadyRegisteredEmail(user)
    sendEmail(Messages(AlreadyRegisteredSubject), user.email.get, txtAndHtml)

  }

  def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].signUpEmail(token)
    sendEmail(Messages(SignUpEmailSubject), to, txtAndHtml)
  }

  def sendWelcomeEmail(user: User)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].welcomeEmail(user)
    sendEmail(Messages(WelcomeEmailSubject), user.email.get, txtAndHtml)

  }

  def sendPasswordResetEmail(user: User, token: String)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].sendPasswordResetEmail(user, token)
    sendEmail(Messages(PasswordResetSubject), user.email.get, txtAndHtml)
  }

  def sendUnkownEmailNotice(email: String)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].unknownEmailNotice()
    sendEmail(Messages(UnknownEmailNoticeSubject), email, txtAndHtml)
  }

  def sendPasswordChangedNotice(user: User)(implicit request: RequestHeader, lang: Lang) {
    val txtAndHtml = use[TemplatesPlugin].passwordChangedNoticeEmail(user)
    sendEmail(Messages(PasswordResetOkSubject), user.email.get, txtAndHtml)
  }

  def sendEmail(subject: String, recipient: String, body: (Option[Txt], Option[Html])) {
    import com.typesafe.plugin._
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Execution.Implicits._

    if (!RegValidator.isEmail(recipient)) {
      Logger.error("[dreampie] error validate recipient=[%s]".format(recipient))
      throw new ValidateException();
    }

    if (Logger.isDebugEnabled) {
      Logger.debug("[dreampie] sending email to %s".format(recipient))
      Logger.debug("[dreampie] mail = [%s]".format(body))
    }

    Akka.system.scheduler.scheduleOnce(1.seconds) {
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.setRecipient(recipient)
      mail.setFrom(fromAddress)
      // the mailer plugin handles null / empty string gracefully
      mail.send(body._1.map(_.body).getOrElse(""), body._2.map(_.body).getOrElse(""))
    }
  }
}
