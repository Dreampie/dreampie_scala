package utils.validator

/**
 * Created by wangrenhui on 14-2-20.
 */
trait Validator {
  def isEmail(email: String): Boolean

  def isMobile(mobile: String): Boolean
}

object RegValidator extends Validator {

  override def isEmail(email: String): Boolean =
    email.matches( """^([a-z0-9A-Z]+[-|\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\.)+[a-zA-Z]{2,}$""")

  //    """^([a-z0-9A-Z]+[-|\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\.)+[a-zA-Z]{2,}$""".r.unapplySeq(email).isDefined


  override def isMobile(mobile: String): Boolean =
    mobile.matches( """^(((13[0-9]{1})|(15[0-9]{1})|(18[0-9]{1}))+\d{8})$""")

  //    """^(((13[0-9]{1})|(15[0-9]{1})|(18[0-9]{1}))+\d{8})$""".r.unapplySeq(mobile).isDefined
}
