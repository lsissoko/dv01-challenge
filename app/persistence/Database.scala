package persistence

import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object DbConfig {
  val db = Database.forConfig("sqlite")
}

object LoanStatsTable {
  class LoanStats(tag: Tag) extends Table[(Long, Int)](tag, "loan_stats") {
    def id = column[Long]("id", O.PrimaryKey)
    def loanAmount = column[Int]("loan_amnt")
    override def * = (id, loanAmount)
  }

  val query = TableQuery[LoanStats]
}

object LoanStatsDAO {
  import DbConfig.db
  import LoanStatsTable.query

  def find(limit: Int): Future[Seq[(Long, Int)]] = {
    // A negative limit would make the SELECT return every row
    val nonNegativeLimit = Math.max(limit, 0)

    db.run(query.take(nonNegativeLimit).result)
  }
}
