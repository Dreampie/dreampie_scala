package controllers.demo

import play.api._, mvc._
import play.api.data._, Forms._, validation.Constraints._

import org.json4s._
import models.demo._
import com.github.tototoshi.play2.json4s.jackson._

object CompanyController extends Controller with Json4s {

  implicit val formats = DefaultFormats

  def all = Action {
    Ok(Extraction.decompose(Company.findAll))
  }

  def show(id: Long) = Action {
    Company.find(id).map {
      company => Ok(Extraction.decompose(company))
    } getOrElse NotFound
  }

  case class CompanyForm(name: String, url: Option[String] = None)

  private val companyForm = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "url" -> optional(text)
    )(CompanyForm.apply)(CompanyForm.unapply)
  )

  def create = Action {
    implicit req =>
      companyForm.bindFromRequest.fold(
        formWithErrors => BadRequest("invalid parameters"),
        form => {
          val company = Company.create(name = form.name, url = form.url)
          Created.withHeaders(LOCATION -> s"/companies/${company.id}")
        }
      )
  }

  def delete(id: Long) = Action {
    Company.find(id).map {
      company =>
        company.destroy()
        NoContent
    } getOrElse NotFound
  }

}
