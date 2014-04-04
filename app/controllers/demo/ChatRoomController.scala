package controllers.demo

import play.api._, mvc._
import play.api.libs.json._

import models.demo._

/**
 * Created by wangrenhui on 14-2-14.
 */
object ChatRoomController extends Controller {
  def index = Action {
    implicit request =>
      Ok(views.html.demo.index())
  }

  /**
   * Display the chat room page.
   */
  def chatRoom(username: Option[String]) = Action {
    implicit request =>
      username.filterNot(_.isEmpty).map {
        username =>
          Ok(views.html.demo.chatRoom(username))
      }.getOrElse {
        Redirect(controllers.demo.routes.ChatRoomController.index).flashing(
          "error" -> "Please choose a valid username."
        )
      }
  }

  def chatRoomJs(username: String) = Action {
    implicit request =>
      Ok(views.js.demo.chatRoom(username))
  }

  /**
   * Handles the chat websocket.
   */
  def chat(username: String) = WebSocket.async[JsValue] {
    request =>

      ChatRoom.join(username)

  }

}
