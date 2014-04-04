package models.security

import org.joda.time.DateTime
import scalikejdbc._, SQLInterpolation._

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

case class Role(id: Long,
                name: String,
                value: String,
                intro: Option[String],
                createdAt: DateTime,
                updatedAt: Option[DateTime] = None,
                deletedAt: Option[DateTime] = None,
                permissions: Seq[Permission] = Nil) extends be.objectify.deadbolt.core.models.Role {
  def getName: String = value

  private val (rp, r, p) = (RolePermission.rp, Role.r, Permission.p)
  private val column = RolePermission.column

  def addPermissionId(permissionId: Long)(implicit session: DBSession = Role.autoSession): Unit = withSQL {
    insert.into(RolePermission)
      .namedValues(column.roleId -> id, column.permissionId -> permissionId)
  }.update.apply()

  def addPermission(permission: Permission)(implicit session: DBSession = Role.autoSession): Unit = withSQL {
    insert.into(RolePermission)
      .namedValues(column.roleId -> id, column.permissionId -> permission.id)
  }.update.apply()

  def deletePermissionId(permissionId: Long)(implicit session: DBSession = Role.autoSession): Unit = withSQL {
    delete.from(RolePermission)
      .where.eq(column.roleId, id).and.eq(column.permissionId, permissionId)
  }.update.apply()

  def deletePermission(permission: Permission)(implicit session: DBSession = Role.autoSession): Unit = withSQL {
    delete.from(RolePermission)
      .where.eq(column.roleId, id).and.eq(column.permissionId, permission.id)
  }.update.apply()
}

object Role extends SQLSyntaxSupport[Role] {
  override val tableName = "sec_role"

  // simple extractor
  def apply(r: SyntaxProvider[Role])(rs: WrappedResultSet): Role = apply(r.resultName)(rs)

  def apply(r: ResultName[Role])(rs: WrappedResultSet): Role = new Role(
    id = rs.long(r.id),
    name = rs.string(r.name),
    value = rs.string(r.value),
    intro = rs.stringOpt(r.intro),
    createdAt = rs.timestamp(r.createdAt).toDateTime,
    updatedAt = rs.timestampOpt(r.updatedAt).map(_.toDateTime),
    deletedAt = rs.timestampOpt(r.deletedAt).map(_.toDateTime)
  )

  def opt(r: SyntaxProvider[Role])(rs: WrappedResultSet): Option[Role] = rs.longOpt(r.resultName.id).map(_ => apply(r.resultName)(rs))

  // SyntaxProvider objects
  val r = Role.syntax("r")
  val (p, rp) = (Permission.p, RolePermission.rp)
  // reusable part of SQL
  private val isNotDeleted = sqls.isNull(r.deletedAt)

  def find(id: Long)(implicit session: DBSession = autoSession): Option[Role] = {
    withSQL {
      select
        .from[Role](Role as r)
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.eq(r.id, id).and.append(isNotDeleted)
    }.one(Role(r))
      .toMany(Permission.opt(p))
      .map {
      (role, permissions) => role.copy(permissions = permissions)
    }.single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Role] = {
    withSQL {
      select
        .from[Role](Role as r)
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.append(isNotDeleted)
    }.one(Role(r))
      .toMany(Permission.opt(p))
      .map {
      (role, permissions) => role.copy(permissions = permissions)
    }.list().apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Role as r)).map(rs => rs.long(1)).single.apply().get
  }

  def create(name: String,
             value: String,
             intro: Option[String],
             createdAt: DateTime)(implicit session: DBSession = autoSession): Role = {
    if (name.isEmpty || value.isEmpty) {
      throw new IllegalArgumentException(s"name,value is empty! (name: ${name},value: ${value})")
    }

    val id = withSQL {
      insert.into(Role).namedValues(
        column.name -> name,
        column.value -> value,
        column.intro -> intro,
        column.createdAt -> createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    Role(
      id = id,
      name = name,
      value = value,
      intro = intro,
      createdAt = createdAt
    )
  }
}