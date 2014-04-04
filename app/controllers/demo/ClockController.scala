package controllers.demo

import play.api._, mvc._
import play.api.libs.Comet
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by wangrenhui on 14-2-14.
 */
object ClockController extends Controller {
  /**
   * A String Enumerator producing a formatted Time message every 100 millis.
   * A callback enumerator is pure an can be applied on several Iteratee.
   */
  lazy val clock: Enumerator[String] = {

    import java.util._
    import java.text._

    val dateFormat = new SimpleDateFormat("HH mm ss")

    Enumerator.generateM {
      Promise.timeout(Some(dateFormat.format(new Date)), 100 milliseconds)
    }
  }

  def liveClock = Action {
    Ok.chunked(clock &> Comet(callback = "parent.clockChanged"))
  }
}
