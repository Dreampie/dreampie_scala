/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package utils

import play.api.mvc.Call
import play.Play
import play.Logger
import scala.language.reflectiveCalls

/**
 *
 */
object RoutesHelper {

  lazy val conf = play.api.Play.current.configuration

  // ProviderController
  lazy val pc = Play.application().classloader().loadClass("controllers.security.ReverseProviderController")
  lazy val providerControllerMethods = pc.newInstance().asInstanceOf[{
    def authenticateByPost(p: String, redirectTo: Option[String]): Call
    def authenticate(p: String, redirectTo: Option[String]): Call
    def notAuthorized: Call
  }]

  def authenticateByPost(provider:String, redirectTo: Option[String] = None): Call = providerControllerMethods.authenticateByPost(provider, redirectTo)
  def authenticate(provider:String, redirectTo: Option[String] = None): Call = providerControllerMethods.authenticate(provider, redirectTo)
  def notAuthorized: Call = providerControllerMethods.notAuthorized

  // LoginPage
  lazy val uc = Play.application().classloader().loadClass("controllers.security.ReverseUserController")
  lazy val userControllerMethods = uc.newInstance().asInstanceOf[{
    def logout(): Call
    def login(): Call
  }]

  def login() = userControllerMethods.login()
  def logout() = userControllerMethods.logout()


//  ///
  lazy val rr = Play.application().classloader().loadClass("controllers.security.ReverseRegistration")
  lazy val registrationMethods = rr.newInstance().asInstanceOf[{
    def handleStartResetPassword(): Call
    def handleStartSignUp(): Call
    def handleSignUp(token:String): Call
    def startSignUp(): Call
    def resetPassword(token:String): Call
    def startResetPassword(): Call
    def signUp(token:String): Call
    def handleResetPassword(token:String): Call
  }]

  def handleStartResetPassword() = registrationMethods.handleStartResetPassword()
  def handleStartSignUp() = registrationMethods.handleStartSignUp()
  def handleSignUp(token:String) = registrationMethods.handleSignUp(token)
  def startSignUp() = registrationMethods.startSignUp()
  def resetPassword(token:String) = registrationMethods.resetPassword(token)
  def startResetPassword() = registrationMethods.startResetPassword()
  def signUp(token:String) = registrationMethods.signUp(token)
  def handleResetPassword(token:String) = registrationMethods.handleResetPassword(token)

  ////
  lazy val passChange = Play.application().classloader().loadClass("controllers.ReversePasswordChange")
  lazy val passwordChangeMethods = passChange.newInstance().asInstanceOf[{
    def page(): Call
    def handlePasswordChange(): Call
  }]

  def changePasswordPage() = passwordChangeMethods.page()
  def handlePasswordChange() = passwordChangeMethods.handlePasswordChange()

  lazy val assets = {
    val clazz = conf.getString("assetsController").getOrElse("controllers.ReverseAssets")
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[dreampie] assets controller = %s".format(clazz))
    }
    Play.application().classloader().loadClass(clazz)
  }

  private lazy val assetsPath = conf.getString("assetsPath").getOrElse("/public")
  private type SimpleAt = { def at(file: String): Call }
  private type AtWithPath = { def at(path: String, file: String): Call }
  private case class AtHelper(impl: AtWithPath)  {
    def at(file: String): Call = impl.at(assetsPath, file)
  }

  lazy val assetsControllerMethods: SimpleAt = {
    val instance = assets.newInstance()
    try {
      instance.getClass.getMethod("at", classOf[String], classOf[String])
      AtHelper(instance.asInstanceOf[AtWithPath])
    } catch {
      case e: NoSuchMethodException => instance.asInstanceOf[SimpleAt]
    }
  }

  def at(file: String) = assetsControllerMethods.at(file)


}
