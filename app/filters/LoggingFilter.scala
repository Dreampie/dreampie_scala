package filters

import play.api.mvc._
import play.api._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by wangrenhui on 14-3-5.
 */
object LoggingFilter extends EssentialFilter {
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val startTime = System.currentTimeMillis
      nextFilter(requestHeader).map {
        result =>
          val endTime = System.currentTimeMillis
          val requestTime = endTime - startTime
          Logger.info(s"${requestHeader.method} ${requestHeader.uri}" +
            s" took ${requestTime}ms and returned ${result.header.status}")
          result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
  }
}