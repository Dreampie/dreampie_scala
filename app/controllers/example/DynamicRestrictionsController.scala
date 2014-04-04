package controllers.example

import play.api.mvc.{Action, Controller}
import views.html.accessOk
import handlers.security.{MyAlternativeDynamicResourceHandler, MyDeadboltHandler}
import be.objectify.deadbolt.scala.DeadboltActions

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
object DynamicRestrictionsController extends Controller with DeadboltActions {
  def pureLuck = Dynamic("pureLuck", "", new MyDeadboltHandler) {
    Action {
      Ok(accessOk())
    }
  }

  def testLuck = Dynamic("testLuck", "", new MyDeadboltHandler) {
    Action {
      Ok(accessOk())
    }
  }

  def noWayJose = Dynamic("pureLuck", "", new MyDeadboltHandler(Some(MyAlternativeDynamicResourceHandler))) {
    Action {
      Ok(accessOk())
    }
  }
}
