package controllers

import play.api.mvc.{Action, Controller}
import handlers.security.MyDeadboltHandler

object Application extends Controller {
  def index = Action {
    implicit request =>
//      Mailer.sendEmail("a", "wangrenhui1990@hotmail.com", (None, Some(views.html.accessOk())))
      Ok(views.html.index(new MyDeadboltHandler))
  }

}