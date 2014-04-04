package controllers.demo

import play.api.mvc._


/**
 * Created by wangrenhui on 14-2-14.
 */
object DemoController extends Controller {


  def demo = Action {
    Ok(views.html.demo.list())
  }

  def clock = Action {
    Ok(views.html.demo.clock())
  }

}
