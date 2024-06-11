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
    *   * if greater than 100, defaults to 100
    * - `offset: Optional[Int]` - the number of results to skip
    *   * defaults to 0 if null or negative
    * - `sortField: Optional[String]` - the field to sort by
    * - `sortDirection: Optional[Int]` - the sort direction, 1 for ascending, -1 for descending
    *   * if null and `sortField` is defined, defaults to 1
    * - `filters: Map[String, Any]` - the optional query filters
    *   * supported keys are `"state"`, `"grade"`, and `"subGrade"`
    *   * e.g. `{"state": "CA"}` or `{"state": "TX", "grade": "B"}`
    */
  def search() = Action.async { implicit req =>
    req.body.asJson match {
      case None => Future.successful(BadRequest) // TODO validation errors would be nice

      case Some(body) => {
        //- NOTE: Manually parsing the request body after Action.async(parse.json) worked for real http
        //- requests but returned 400 from the controller tests.
        val limit = (body \ "limit").asOpt[Int].getOrElse(10) // Set a default limit
        val offset = (body \ "offset").asOpt[Int].getOrElse(0) // Set a default offset
        val sortField = (body \ "sortField").asOpt[String]
        val sortDirection = (body \ "sortDirection").asOpt[Int]

        val filters = LoanStatsDAO.LoanStatsFilter(
          state = (body \ "filter" \ "state").asOpt[String],
          grade = (body \ "filter" \ "grade").asOpt[String],
          subGrade = (body \ "filter" \ "subGrade").asOpt[String],
        )

        LoanStatsDAO
          .find(limit, offset, sortField, sortDirection, filters)
          .map(result => Ok(Json.toJson(result)))
      }
    }
  }
}
