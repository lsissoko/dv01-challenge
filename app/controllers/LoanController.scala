package controllers

import javax.inject._
import play.api._
import play.api.libs.json._
import play.api.mvc._

import persistence.LoanStatsDAO

@Singleton
class LoanController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def find(limit: Int) = Action.async { implicit req =>
    LoanStatsDAO.find(limit).map { result =>
      Ok(Json.toJson(result))
    }
  }
}
