package models.security

import scalikejdbc._
import scalikejdbc.SQLInterpolation._
import org.joda.time.DateTime

/**
 * A token used for reset password and sign up operations
 */
case class Token(
                  uuid: String,
                  username: String,
                  createdAt: DateTime,
                  expirationAt: DateTime,
                  isSignUp:Boolean) {

  def create()(implicit session: DBSession = Token.autoSession): Token = Token.create(this)

  def save()(implicit session: DBSession = Token.autoSession): Token = Token.save(this)(session)

  def destroy()(implicit session: DBSession = Token.autoSession): Unit = Token.destroy(this)(session)

  def isExpired = expirationAt.isBeforeNow
}


object Token extends SQLSyntaxSupport[Token] {

  override val tableName = "SEC_TOKEN"

  override val columns = Seq("ID", "UUID", "USERNAME", "CREATED_AT", "EXPIRATION_AT", "IS_SIGN_UP")

  def apply(t: ResultName[Token])(rs: WrappedResultSet): Token = new Token(
    uuid = rs.string(t.uuid),
    username = rs.string(t.username),
    createdAt = rs.timestamp(t.createdAt).toDateTime,
    expirationAt = rs.timestamp(t.expirationAt).toDateTime,
    isSignUp = rs.boolean(t.isSignUp)
  )

  val t = Token.syntax("t")

  override val autoSession = AutoSession

  def find(uuid: String)(implicit session: DBSession = autoSession): Option[Token] = {
    withSQL {
      select.from(Token as t).where.eq(t.uuid, uuid)
    }.map(Token(t.resultName)).single.apply()
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[Token] = {
    withSQL {
      select.from(Token as t).where.append(sqls"${where}")
    }.map(Token(t.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Token] = {
    withSQL(select.from(Token as t)).map(Token(t.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Token as t)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Token] = {
    withSQL {
      select.from(Token as t).where.append(sqls"${where}")
    }.map(Token(t.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Token as t).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(token: Token): Token = create(token.uuid, token.username, token.createdAt, token.expirationAt, token.isSignUp)

  def create(
              uuid: String,
              username: String,
              createdAt: DateTime,
              expirationAt: DateTime,
              isSignUp: Boolean)(implicit session: DBSession = autoSession): Token = {
    withSQL {
      insert.into(Token).columns(
        column.uuid,
        column.username,
        column.createdAt,
        column.expirationAt,
        column.isSignUp
      ).values(
          uuid,
          username,
          createdAt,
          expirationAt,
          isSignUp
        )
    }.update().apply()

    Token(
      uuid = uuid,
      username = username,
      createdAt = createdAt,
      expirationAt = expirationAt,
      isSignUp = isSignUp)
  }

  def save(entity: Token)(implicit session: DBSession = autoSession): Token = {
    withSQL {
      update(Token).set(
        column.uuid -> entity.uuid,
        column.username -> entity.username,
        column.createdAt -> entity.createdAt,
        column.expirationAt -> entity.expirationAt,
        column.isSignUp -> entity.isSignUp
      ).where.eq(column.uuid, entity.uuid)
    }.update.apply()
    entity
  }

  def destroy(entity: Token)(implicit session: DBSession = autoSession): Unit = {
    withSQL {
      delete.from(Token).where.eq(column.uuid, entity.uuid)
    }.update.apply()
  }

  def destroyBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Unit = {
    withSQL {
      delete.from(Token).where.append(sqls"${where}")
    }.update.apply()
  }

}
