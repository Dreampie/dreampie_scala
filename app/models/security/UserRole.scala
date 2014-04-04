package models.security

import scalikejdbc._, SQLInterpolation._

/**
 * Created by wangrenhui on 14-2-20.
 */
case class UserRole(id: Long, userId: Long, roleId: Long)

object UserRole extends SQLSyntaxSupport[UserRole] {
  override val tableName = "sec_user_role"
  val ur = UserRole.syntax("ur")
}
