package controllers

import play.api._, mvc._

object JsRouter extends Controller {

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("jsRoutes")(
          controllers.demo.routes.javascript.CompanyController.all,
          controllers.demo.routes.javascript.CompanyController.show,
          controllers.demo.routes.javascript.CompanyController.create,
          controllers.demo.routes.javascript.CompanyController.delete,
          controllers.demo.routes.javascript.ProgrammerController.all,
          controllers.demo.routes.javascript.ProgrammerController.show,
          controllers.demo.routes.javascript.ProgrammerController.create,
          controllers.demo.routes.javascript.ProgrammerController.addSkill,
          controllers.demo.routes.javascript.ProgrammerController.deleteSkill,
          controllers.demo.routes.javascript.ProgrammerController.joinCompany,
          controllers.demo.routes.javascript.ProgrammerController.leaveCompany,
          controllers.demo.routes.javascript.ProgrammerController.delete,
          controllers.routes.javascript.Application.index,
          controllers.demo.routes.javascript.SkillController.all,
          controllers.demo.routes.javascript.SkillController.show,
          controllers.demo.routes.javascript.SkillController.create,
          controllers.demo.routes.javascript.SkillController.delete
        )
      ).as("text/javascript")
  }

}
