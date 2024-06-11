package persistence

import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.json.{Format, Json}
import scala.concurrent.Future

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
    def date = column[String]("issue_d") // TODO convert to DateTime
    def state = column[String]("addr_state")
    def grade = column[String]("grade")
    def subGrade = column[String]("sub_grade")
    def ficoRangeLow = column[Int]("fico_range_low")
    def ficoRangeHigh = column[Int]("fico_range_high")
    override def * = (id, loanAmount.?, date.?, state.?, grade.?, subGrade.?, ficoRangeLow.?, ficoRangeHigh.?).mapTo[LoanStat]
  }

  val query = TableQuery[LoanStats]
}

object LoanStatsDAO {
  import DbConfig.db
  import LoanStatsTable.{LoanStat, query}

  def find(limit: Int): Future[Seq[LoanStat]] = {
    // A negative limit would make the SELECT return every row
    val nonNegativeLimit = Math.max(limit, 0)

    db.run(query.take(nonNegativeLimit).result)
  }
}
