package models.security

import _root_.play.libs.Scala
import be.objectify.deadbolt.core.models.Subject
import org.joda.time.DateTime
import scalikejdbc._, SQLInterpolation._
import utils.validator._
import secure._
import secure.providers.UsernamePasswordProvider

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

case class User(id: Long,
                username: String,
                providername: String,
                email: Option[String],
                mobile: Option[String],
                password: String,
                hasher: String,
                salt: String,
                avatarUrl: Option[String],
                firstName: String,
                lastName: String,
                fullName: String,
                createdAt: DateTime,
                updatedAt: Option[DateTime] = None,
                deletedAt: Option[DateTime] = None,
                userInfo: Option[UserInfo] = None,
                roles: Seq[Role] = Nil,
                permissions: Seq[Permission] = Nil,
                authMethod: AuthenticationMethod,
                oAuth1Info: Option[OAuth1Info] = None,
                oAuth2Info: Option[OAuth2Info] = None) extends Subject {

  def find()(implicit session: DBSession = User.autoSession): Option[User] = User.find(id)(session)

  def create()(implicit session: DBSession = User.autoSession): User = User.create(this)

  def save()(implicit session: DBSession = User.autoSession): User = User.save(this)(session)

  def destroy()(implicit session: DBSession = User.autoSession): Unit = User.destroy(id)(session)

  def getRoles: java.util.List[Role] = {
    Scala.asJava(roles)
  }

  def getPermissions: java.util.List[Permission] = {
    Scala.asJava(permissions)
  }

  def getIdentifier: String = username

  private val (ur, u, r) = (UserRole.ur, User.u, Role.r)
  private val column = UserRole.column

  def addRoleId(roleId: Long)(implicit session: DBSession = User.autoSession): Unit = withSQL {
    insert.into(UserRole)
      .namedValues(column.userId -> id, column.roleId -> roleId)
  }.update.apply()

  def addRole(role: Role)(implicit session: DBSession = User.autoSession): Unit = withSQL {
    insert.into(UserRole)
      .namedValues(column.userId -> id, column.roleId -> role.id)
  }.update.apply()


  def deleteRoleId(roleId: Long)(implicit session: DBSession = User.autoSession): Unit = withSQL {
    delete.from(UserRole)
      .where.eq(column.userId, id).and.eq(column.roleId, roleId)
  }.update.apply()

  def deleteRole(role: Role)(implicit session: DBSession = User.autoSession): Unit = withSQL {
    delete.from(UserRole)
      .where.eq(column.userId, id).and.eq(column.roleId, role.id)
  }.update.apply()
}

object User extends SQLSyntaxSupport[User] {

  override val tableName = "sec_user"
  // simple extractor

  def apply(a: ResultName[User])(rs: WrappedResultSet): User = new User(
    id = rs.long(a.id),
    username = rs.string(a.username),
    providername = rs.string(a.providername),
    email = rs.stringOpt(a.email),
    mobile = rs.stringOpt(a.mobile),
    hasher = rs.string(a.hasher),
    password = rs.string(a.password),
    salt = rs.string(a.salt),
    avatarUrl = rs.stringOpt(a.avatarUrl),
    firstName = rs.string(a.firstName),
    lastName = rs.string(a.lastName),
    fullName = rs.string(a.fullName),
    createdAt = rs.timestamp(a.createdAt).toDateTime,
    updatedAt = rs.timestampOpt(a.updatedAt).map(_.toDateTime),
    deletedAt = rs.timestampOpt(a.deletedAt).map(_.toDateTime),
    authMethod = AuthenticationMethod.UserPassword,
    oAuth1Info = None,
    oAuth2Info = None
  )

  // join query with info table
  def apply(u: SyntaxProvider[User], i: SyntaxProvider[UserInfo])(rs: WrappedResultSet): User = {
    apply(u.resultName)(rs).copy(userInfo = rs.longOpt(i.resultName.id).flatMap {
      _ =>
        if (rs.timestampOpt(i.resultName.deletedAt).isEmpty) Some(UserInfo(i)(rs)) else None
    })
  }

  // SyntaxProvider objects
  val u = User.syntax("u")

  private val (i, r, ur, p, rp) = (UserInfo.i, Role.r, UserRole.ur, Permission.p, RolePermission.rp)

  // reusable part of SQL
  private val isNotDeleted = sqls.isNull(u.deletedAt)

  def find(id: Long)(implicit session: DBSession = autoSession): Option[User] = {
    withSQL {
      select
        .from[User](User as u)
        .leftJoin(UserInfo as i).on(sqls.eq(i.userId, u.id).and.isNull(i.deletedAt))
        .leftJoin(UserRole as ur).on(ur.userId, u.id)
        .leftJoin(Role as r).on(sqls.eq(ur.roleId, r.id).and.isNull(r.deletedAt))
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.eq(u.id, id).append(isNotDeleted)
    }.one(User(u, i)).toManies(
        rs => Role.opt(r)(rs),
        rs => Permission.opt(p)(rs)
      ).map {
      (user, roles, permissions) => user.copy(roles = roles, permissions = permissions)
    }.single.apply()
  }

  def findByUsername(username: String)(implicit session: DBSession = autoSession): Option[User] = {
    withSQL {
      select
        .from[User](User as u)
        .leftJoin(UserInfo as i).on(sqls.eq(i.userId, u.id).and.isNull(i.deletedAt))
        .leftJoin(UserRole as ur).on(ur.userId, u.id)
        .leftJoin(Role as r).on(sqls.eq(ur.roleId, r.id).and.isNull(r.deletedAt))
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.map(
          sql =>
            if (RegValidator.isEmail(username)) sql.eq(u.email, username)
            else if (RegValidator.isMobile(username)) sql.eq(u.mobile, username)
            else sql.eq(u.username, username)
        ).and.append(isNotDeleted)
    }.one(User(u, i)).toManies(
        rs => Role.opt(r)(rs),
        rs => Permission.opt(p)(rs)
      ).map {
      (user, roles, permissions) => user.copy(roles = roles, permissions = permissions)
    }.single.apply()
  }

  def findByUserProvider(username: String, providername: String)(implicit session: DBSession = autoSession): Option[User] = {
    withSQL {
      select
        .from[User](User as u)
        .leftJoin(UserInfo as i).on(sqls.eq(i.userId, u.id).and.isNull(i.deletedAt))
        .leftJoin(UserRole as ur).on(ur.userId, u.id)
        .leftJoin(Role as r).on(sqls.eq(ur.roleId, r.id).and.isNull(r.deletedAt))
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.map(
          sql =>
            if (RegValidator.isEmail(username)) sql.eq(u.email, username)
            else if (RegValidator.isMobile(username)) sql.eq(u.mobile, username)
            else sql.eq(u.username, username)
        ).and.eq(u.providername, providername).and.append(isNotDeleted)
    }.one(User(u, i)).toManies(
        rs => Role.opt(r)(rs),
        rs => Permission.opt(p)(rs)
      ).map {
      (user, roles, permissions) => user.copy(roles = roles, permissions = permissions)
    }.single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[User] = {
    withSQL {
      select
        .from[User](User as u)
        .leftJoin(UserInfo as i).on(sqls.eq(i.userId, u.id).and.isNull(i.deletedAt))
        .leftJoin(UserRole as ur).on(ur.userId, u.id)
        .leftJoin(Role as r).on(sqls.eq(ur.roleId, r.id).and.isNull(r.deletedAt))
        .leftJoin(RolePermission as rp).on(rp.roleId, r.id)
        .leftJoin(Permission as p).on(rp.permissionId, p.id)
        .where.append(isNotDeleted)
    }.one(User(u, i)).toManies(
        rs => Role.opt(r)(rs),
        rs => Permission.opt(p)(rs)
      ).map {
      (user, roles, permissions) => user.copy(roles = roles, permissions = permissions)
    }.list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(User as u)).map(rs => rs.long(1)).single.apply().get
  }

  def create(user: User): User = create(user.username, user.providername, user.email, user.mobile, user.password, user.hasher, user.salt, user.avatarUrl, user.firstName, user.lastName, user.fullName, user.createdAt)

  def create(username: String,
             providername: String,
             email: Option[String],
             mobile: Option[String],
             password: String,
             hasher: String,
             salt: String,
             avatarUrl: Option[String],
             firstName: String,
             lastName: String,
             fullName: String,
             createdAt: DateTime = DateTime.now)(implicit session: DBSession = autoSession): User = {
    if (username.isEmpty || providername.isEmpty || (providername.equals(UsernamePasswordProvider.UsernamePassword) && password.isEmpty)) {
      throw new IllegalArgumentException(s"username,providername,password is empty! (username: ${username},providername:${providername},password: ${password})")
    }
    if (User.findByUserProvider(username, providername).isDefined) {
      throw new IllegalArgumentException(s"username,providername is defined! (username: ${username},providername:${providername})")
    }

    val id = withSQL {
      insert.into(User).namedValues(
        column.username -> username,
        column.providername -> providername,
        column.email -> email,
        column.mobile -> mobile,
        column.password -> password,
        column.hasher -> hasher,
        column.salt -> salt,
        column.avatarUrl -> avatarUrl,
        column.firstName -> firstName,
        column.lastName -> lastName,
        column.fullName -> fullName,
        column.createdAt -> createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    User(
      id = id,
      username = username,
      providername = providername,
      email = email,
      mobile = mobile,
      hasher = hasher,
      password = password,
      salt = salt,
      avatarUrl = avatarUrl,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      createdAt = createdAt,
      authMethod = AuthenticationMethod.UserPassword,
      oAuth1Info = None,
      oAuth2Info = None)
  }

  def save(m: User)(implicit session: DBSession = autoSession): User = {
    withSQL {
      update(User).set(
        column.username -> m.username,
        column.email -> m.email,
        column.mobile -> m.mobile,
        column.hasher -> m.hasher,
        column.password -> m.password,
        column.salt -> m.salt,
        column.avatarUrl -> m.avatarUrl,
        column.firstName -> m.firstName,
        column.lastName -> m.lastName,
        column.fullName -> m.fullName,
        column.updatedAt -> DateTime.now
      ).where.eq(column.id, m.id).and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

  def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(User).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()

}