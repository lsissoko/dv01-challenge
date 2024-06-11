package persistence

import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.json.{Format, Json}
import scala.concurrent.Future
import slick.lifted.{ColumnOrdered, Ordered}
import slick.ast.Ordering
import play.api.libs.json.Reads
import models.LoanStatsFilter

object DbConfig {
  val db = Database.forConfig("sqlite")
}

// TODO this is getting big enough to move to a new file
object LoanStatsTable {
  case class LoanStat(
    id: Long,
    loanAmount: Option[Int],
    date: Option[String], // TODO convert to DateTime
    state: Option[String],
    grade: Option[String],
    subGrade: Option[String],
    ficoRangeLow: Option[Int],
    ficoRangeHigh: Option[Int],
  )

  object LoanStat {
    implicit val format: Format[LoanStat] = Json.format[LoanStat]
  }

  class LoanStats(tag: Tag) extends Table[LoanStat](tag, "loan_stats") {
    def id = column[Long]("id", O.PrimaryKey)
    def loanAmount = column[Int]("loan_amnt")
    // TODO convert to DateTime (string/alpha sorting is wrong, e.g. "Oct-2017" should be before "Dec-2017")
    def date = column[String]("issue_d")
    def state = column[String]("addr_state")
    def grade = column[String]("grade")
    def subGrade = column[String]("sub_grade")
    def ficoRangeLow = column[Int]("fico_range_low")
    def ficoRangeHigh = column[Int]("fico_range_high")

    val sortFields: Map[String, Rep[_]] = Map(
      "id" -> this.id,
      "loanAmount" -> this.loanAmount,
      "date" -> this.date,
      "state" -> this.state,
      "grade" -> this.grade,
      "subGrade" -> this.subGrade,
      "ficoRangeLow" -> this.ficoRangeLow,
      "ficoRangeHigh" -> this.ficoRangeHigh,
    )

    override def * = (id, loanAmount.?, date.?, state.?, grade.?, subGrade.?, ficoRangeLow.?, ficoRangeHigh.?).mapTo[LoanStat]
  }

  val query = TableQuery[LoanStats]
}

object LoanStatsDAO {
  import DbConfig.db
  import LoanStatsTable.{LoanStat, query}
  import models.PaginatedResult

  def find(
      limit: Int,
      offset: Int = 0,
      maybeSortField: Option[String] = None,
      maybeSortDirection: Option[Int] = None,
      maybeFilter: Option[LoanStatsFilter] = None,
  ): Future[PaginatedResult[LoanStat]] = {
    val minLimit = 0
    val maxLimit = 100
    val boundedLimit = Math.min(Math.max(limit, minLimit), maxLimit)

    val sortedQuery = maybeSortField match {
      case None => query
      case Some(fieldName) => {
        val direction = maybeSortDirection.getOrElse(1)
        val ordering = if (direction == -1) Ordering.Desc else Ordering.Asc
        val sortOrderRep: Rep[_] => Ordered = ColumnOrdered(_, Ordering(ordering))
        query.sortBy(_.sortFields(fieldName))(sortOrderRep)
      }
    }

    //- Conditional filtering ignores the null filters.<name> values
    val filters = maybeFilter.getOrElse(LoanStatsFilter())
    val sortedAndFilteredQuery = sortedQuery
      .filterOpt(filters.state)(_.state === _)
      .filterOpt(filters.grade)(_.grade === _)
      .filterOpt(filters.subGrade)(_.subGrade === _)

    db.run {
      for {
        totalCount <- sortedAndFilteredQuery.length.result
        entities <- sortedAndFilteredQuery.drop(offset).take(boundedLimit).result
      } yield PaginatedResult(
        totalCount,
        entities,
        hasNextPage = totalCount - (offset + limit) > 0
      )
    }
  }
}
