package models.security

import org.joda.time.DateTime
import scalikejdbc._, SQLInterpolation._

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

case class Permission(id: Long,
                      name: String,
                      value: String,
                      url: Option[String],
                      intro: Option[String],
                      createdAt: DateTime,
                      updatedAt: Option[DateTime] = None,
                      deletedAt: Option[DateTime] = None) extends be.objectify.deadbolt.core.models.Permission {
  def getValue: String = value
}

object Permission extends SQLSyntaxSupport[Permission] {
  override val tableName = "sec_permission"

  // simple extractor
  def apply(p: SyntaxProvider[Permission])(rs: WrappedResultSet): Permission = apply(p.resultName)(rs)

  def apply(p: ResultName[Permission])(rs: WrappedResultSet): Permission = new Permission(
    id = rs.long(p.id),
    name = rs.string(p.name),
    value = rs.string(p.value),
    url = rs.stringOpt(p.url),
    intro = rs.stringOpt(p.intro),
    createdAt = rs.timestamp(p.createdAt).toDateTime,
    updatedAt = rs.timestampOpt(p.updatedAt).map(_.toDateTime),
    deletedAt = rs.timestampOpt(p.deletedAt).map(_.toDateTime)
  )

  def opt(p: SyntaxProvider[Permission])(rs: WrappedResultSet): Option[Permission] = rs.longOpt(p.resultName.id).map(_ => apply(p.resultName)(rs))

  // SyntaxProvider objects
  val p = Permission.syntax("p")
  // reusable part of SQL
  private val isNotDeleted = sqls.isNull(p.deletedAt)

  def find(id: Long)(implicit session: DBSession = autoSession): Option[Permission] = {
    withSQL {
      select
        .from[Permission](Permission as p)
        .where.eq(p.id, id).and.append(isNotDeleted)
    }.map(Permission(p)).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Permission] = {
    withSQL {
      select
        .from[Permission](Permission as p)
        .where.append(isNotDeleted)
    }.map(Permission(p)).list().apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Permission as p)).map(rs => rs.long(1)).single.apply().get
  }

  def create(name: String,
             value: String,
             url: Option[String],
             intro: Option[String],
             createdAt: DateTime)(implicit session: DBSession = autoSession): Permission = {
    if (name.isEmpty || value.isEmpty) {
      throw new IllegalArgumentException(s"name,value is empty! (name: ${name},value: ${value})")
    }

    val id = withSQL {
      insert.into(Permission).namedValues(
        column.name -> name,
        column.value -> value,
        column.url -> url,
        column.intro -> intro,
        column.createdAt -> createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    Permission(
      id = id,
      name = name,
      value = value,
      url = url,
      intro = intro,
      createdAt = createdAt
    )
  }
}