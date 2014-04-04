package controllers.demo

import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import scala.concurrent.duration._
import akka.util.Timeout

import socketio.PacketTypes._
import socketio._
import play.api.mvc.Action

object SocketIOController extends Controller with SocketIOActions {

  def welcome = Action {
    implicit request =>
      Ok(views.html.demo.welcome())
  }

  val clientTimeout = Timeout(10.seconds)

  def processMessage(sessionId: String, packet: Packet) {

    packet.packetType match {
      //Process regular message
      case (MESSAGE) => {
        println("Processed request for sessionId: " + packet.data)
        //DO your message processing here
        enqueueMsg(sessionId, "Processed request for sessionId: " + packet.data)
      }

      //Process JSON message
      case (JSON) => {
        println("Processed request for sessionId: " + packet.data)
        //
        enqueueJsonMsg(sessionId, "Processed request for sessionId: " + packet.data)
      }

      //Handle event
      case (EVENT) => {
        println("Processed request for sessionId: " + packet.data)
        // "Processed request for sessionId: " + eventData
        val parsedData: JsValue = Json.parse(packet.data)
        (parsedData \ "name").asOpt[String] match {
          case Some("my other event") => {
            val data = parsedData \ "args"
            //process data
            println( """"my other event" just happened for client: """ + sessionId + " data sent: " + data)
            enqueueEvent(sessionId, "Processed request for sessionId: " + data)
          }
          case _ =>
            println("Unkown event happened.")
        }
      }

    }

  }
}
