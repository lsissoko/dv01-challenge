package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future

import models.LoanStatsSearch
import persistence.LoanStatsDAO

@Singleton
class LoanController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
    * Returns loan stats from the database matching the given [[LoanStatsSearch]].
    *
    * The POST request's JSON body is a [[LoanStatsSearch]] with:
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
  def search() = Action.async(parse.json) { implicit req =>
    req.body.asOpt[LoanStatsSearch] match {
      case None => Future.successful(BadRequest) //- TODO validation errors would be nice
      case Some(body) => LoanStatsDAO
        .find(
          limit = body.limit.getOrElse(10),
          offset = body.offset.getOrElse(0),
          maybeSortField = body.sortField,
          maybeSortDirection = body.sortDirection,
          maybeFilter = body.filter,
        )
        .map(result => Ok(Json.toJson(result)))
    }
  }

  /**
    * Returns aggregate data for a continuous variable grouped by a categorical variable,
    * e.g. loan amount by state
    *
    * @param selectField (query string param) the continuous variable to aggregate
    * @param groupByField (query string param) the cateogorical variable to group by
    * @return list of [[models.BasicLoanStatsResult]]
    */
  def aggregate(selectField: String, groupByField: String) = Action.async { implicit req =>
    //- TODO extract to a Map or method, maybe in a service class
    //- TODO support more variables
    // Must be a continuous variable
    val selectColName = Some(selectField) match {
      case Some("loanAmount") => "loan_amnt"
      case _ => ""
    }
    // Must be a categorical variable
    val groupByColName = Some(groupByField) match {
      case Some("state") => "addr_state"
      case Some("grade") => "grade"
      case _ => ""
    }

    //- TODO better request body validation
    if (selectColName.isEmpty()) {
      Future.successful(BadRequest("invalid selectField"))
    } else if (groupByColName.isEmpty()) {
      Future.successful(BadRequest("invalid groupByField"))
    } else {
    LoanStatsDAO
      .aggregate(selectColName, groupByColName)
      .map(result => Ok(Json.toJson(result)))
    }
  }
}
