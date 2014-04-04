package models.security

import scalikejdbc._, SQLInterpolation._

/**
 * Created by wangrenhui on 14-2-20.
 */
case class RolePermission(id: Long, roleId: Long, permissionId: Long)

object RolePermission extends SQLSyntaxSupport[RolePermission] {
  override val tableName = "sec_role_permission"
  val rp = RolePermission.syntax("rp")
}