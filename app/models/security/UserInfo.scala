package models.security

import org.joda.time.DateTime
import scalikejdbc._, SQLInterpolation._

/**
 * Created by wangrenhui on 14-2-19.
 */
case class UserInfo(
                     id: Long,
                     userId: Long,
                     creatorId: Option[Long],
                     gender: Int,
                     createdAt: DateTime,
                     updatedAt: Option[DateTime] = None,
                     deletedAt: Option[DateTime] = None
                     ) {
  def save()(implicit session: DBSession = UserInfo.autoSession): UserInfo = UserInfo.save(this)(session)

  def destroy()(implicit session: DBSession = UserInfo.autoSession): Unit = UserInfo.destroy(id)(session)
}

object UserInfo extends SQLSyntaxSupport[UserInfo] {
  override val tableName = "sec_user_info"

  // simple extractor
  def apply(i: SyntaxProvider[UserInfo])(rs: WrappedResultSet): UserInfo = apply(i.resultName)(rs)

  def apply(i: ResultName[UserInfo])(rs: WrappedResultSet): UserInfo = new UserInfo(
    id = rs.long(i.id),
    userId = rs.long(i.userId),
    creatorId = rs.longOpt(i.creatorId),
    gender = rs.int(i.gender),
    createdAt = rs.timestamp(i.createdAt).toDateTime,
    updatedAt = rs.timestampOpt(i.updatedAt).map(_.toDateTime),
    deletedAt = rs.timestampOpt(i.deletedAt).map(_.toDateTime)
  )

  def opt(i: SyntaxProvider[UserInfo])(rs: WrappedResultSet): Option[UserInfo] = rs.longOpt(i.resultName.id).map(_ => apply(i.resultName)(rs))

  // SyntaxProvider objects
  val i = UserInfo.syntax("i")
  // reusable part of SQL
  private val isNotDeleted = sqls.isNull(i.deletedAt)

  def find(id: Long)(implicit session: DBSession = autoSession): Option[UserInfo] = withSQL {
    select.from(UserInfo as i).where.eq(i.id, id)
  }.map(UserInfo(i)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[UserInfo] = withSQL {
    select.from(UserInfo as i)
      .where.append(isNotDeleted)
      .orderBy(i.id)
  }.map(UserInfo(i)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(UserInfo as i).where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[UserInfo] = withSQL {
    select.from(UserInfo as i)
      .where.append(isNotDeleted).and.append(sqls"${where}")
      .orderBy(i.id)
  }.map(UserInfo(i)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(UserInfo as i).where.append(isNotDeleted).and.append(sqls"${where}")
  }.map(_.long(1)).single.apply().get

  def create(info: UserInfo): UserInfo = create(userId = info.userId, creatorId = info.creatorId, gender = info.gender, createdAt = info.createdAt)


  def create(userId: Long, creatorId: Option[Long], gender: Int, createdAt: DateTime = DateTime.now)(implicit session: DBSession = autoSession): UserInfo = {
    val id = withSQL {
      insert.into(UserInfo).namedValues(
        column.userId -> userId,
        column.creatorId -> creatorId,
        column.gender -> gender,
        column.createdAt -> createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    UserInfo(id = id, userId = userId, creatorId = creatorId, gender = gender, createdAt = createdAt)
  }

  def save(m: UserInfo)(implicit session: DBSession = autoSession): UserInfo = {
    withSQL {
      update(UserInfo).set(
        column.userId -> m.userId,
        column.updatedAt -> m.updatedAt
      ).where.eq(column.id, m.id).and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

  def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(UserInfo).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()
}