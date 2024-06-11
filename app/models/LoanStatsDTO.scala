package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.language.postfixOps

case class LoanStatsSearch(
  limit: Option[Int],
  offset: Option[Int],
  sortField: Option[String],
  sortDirection: Option[Int],
  filter: Option[LoanStatsFilter]
)

object LoanStatsSearch {
  implicit val reads: Reads[LoanStatsSearch] = Json.reads[LoanStatsSearch]
}

case class LoanStatsFilter (
  // TODO add more equality filters
  state: Option[String] = None,
  grade: Option[String] = None,
  subGrade: Option[String] = None,

  // TODO add range filters (maybe in a separate case class), e.g. minGrade and maxGrade
)

object LoanStatsFilter {
  implicit val reads: Reads[LoanStatsFilter] = Json.reads[LoanStatsFilter]
}
