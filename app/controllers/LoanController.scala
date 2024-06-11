package controllers

import javax.inject._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.Future

import persistence.LoanStatsDAO

@Singleton
class LoanController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def find(limit: Int) = Action.async { implicit req =>
    LoanStatsDAO.find(limit).map { result =>
      Ok(Json.toJson(result))
    }
  }

  /**
    * Returns loan stats from the database matching the given criteria.
    *
    * The POST request accepts criteria in a JSON body with the following fields:
    * - `limit: Optional[Int]` - the maximum number of results to return
    *   * if null, defaults to 10
    *   * if negative, defaults to 0
    * - `sortField: Optional[String]` - the field to sort by
    * - `sortDirection: Optional[Int]` - the sort direction, 1 for ascending, -1 for descending
    *   * if null and `sortField` is defined, defaults to 1
    */
  def search() = Action.async { implicit req =>
    req.body.asJson match {
      case None => Future.successful(BadRequest) // TODO validation errors would be nice

      case Some(body) => {
        //- NOTE: Manually parsing the request body after Action.async(parse.json) worked for real http
        //- requests but returned 400 from the controller tests.
        val limit = (body \ "limit").asOpt[Int].getOrElse(10) // Set a default limit
        val sortField = (body \ "sortField").asOpt[String]
        val sortDirection = (body \ "sortDirection").asOpt[Int]

        LoanStatsDAO
          .find(limit, sortField, sortDirection)
          .map(result => Ok(Json.toJson(result)))
      }
    }
  }
}
