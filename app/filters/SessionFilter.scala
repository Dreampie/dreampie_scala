package filters

import play.api.mvc._

/**
 * Created by wangrenhui on 14-3-5.
 */
object SessionFilter extends EssentialFilter {
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val startTime = System.currentTimeMillis

      nextFilter(requestHeader)
    }
  }
}