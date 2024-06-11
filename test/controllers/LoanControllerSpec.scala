package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import persistence.LoanStatsTable.LoanStat
import org.apache.pekko.actor.ActorSystem
import models.PaginatedResult
import models.BasicLoanStatsResult

class LoanControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val totalCount = 118636

  implicit val actorSystem: ActorSystem = ActorSystem()

  "LoanController POST" should {
    "return an empty list when the limit is negative" in {
      val controller = new LoanController(stubControllerComponents())
      val req = FakeRequest(POST, "/api/loans").withJsonBody(Json.obj("limit" -> -1))
      val actual = call(controller.search(), req)

      val expected = PaginatedResult(totalCount, Seq.empty[LoanStat], hasNextPage = true)

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return an empty list when the limit is zero" in {
      val controller = new LoanController(stubControllerComponents())
      val req = FakeRequest(POST, "/api/loans").withJsonBody(Json.obj("limit" -> 0))
      val actual = call(controller.search(), req)

      val expected = PaginatedResult(totalCount, Seq.empty[LoanStat], hasNextPage = true)

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return the 2 loans with the best grades" in {
      val controller = new LoanController(stubControllerComponents())
      val reqBody = Json.obj("limit" -> 2, "sortField" -> "grade", "sortDirection" -> 1) // ascending sort
      val req = FakeRequest(POST, "/api/loans").withJsonBody(reqBody)
      val actual = call(controller.search(), req)

      val expectedEntities = Seq(
        LoanStat(
          id=126285300,
          loanAmount=Some(40000),
          date=Some("Dec-2017"),
          state=Some("CA"),
          grade=Some("A"),
          subGrade=Some("A2"),
          ficoRangeLow=Some(780),
          ficoRangeHigh=Some(784),
        ),
        LoanStat(
          id=126410177,
          loanAmount=Some(6500),
          date=Some("Dec-2017"),
          state=Some("WV"),
          grade=Some("A"),
          subGrade=Some("A4"),
          ficoRangeLow=Some(705),
          ficoRangeHigh=Some(709),
        ),
      )
      val expected = PaginatedResult(totalCount, expectedEntities, hasNextPage = true)

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return the 2 loans with the worst grades" in {
      val controller = new LoanController(stubControllerComponents())
      val reqBody = Json.obj("limit" -> 2, "sortField" -> "grade", "sortDirection" -> -1) // descending sort
      val req = FakeRequest(POST, "/api/loans").withJsonBody(reqBody)
      val actual = call(controller.search(), req)

      val expectedEntities = Seq(
        LoanStat(
          id=126176997,
          loanAmount=Some(30000),
          date=Some("Dec-2017"),
          state=Some("NY"),
          grade=Some("G"),
          subGrade=Some("G1"),
          ficoRangeLow=Some(700),
          ficoRangeHigh=Some(704),
        ),
        LoanStat(
          id=126068485,
          loanAmount=Some(20000),
          date=Some("Dec-2017"),
          state=Some("VA"),
          grade=Some("G"),
          subGrade=Some("G1"),
          ficoRangeLow=Some(665),
          ficoRangeHigh=Some(669),
        ),
      )
      val expected = PaginatedResult(totalCount, expectedEntities, hasNextPage = true)

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return an empty list when the given grade is invalid" in {
      val controller = new LoanController(stubControllerComponents())
      val reqBody = Json.parse("""{
        "limit": 1,
        "filter": { "grade": "veryFakeGrade" }
      }""")
      val req = FakeRequest(POST, "/api/loans").withJsonBody(reqBody)
      val actual = call(controller.search(), req)

      val expected = PaginatedResult(entities = Seq.empty[LoanStat])

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return the second grade A loan for New Jersey" in {
      val controller = new LoanController(stubControllerComponents())
      val reqBody = Json.parse("""{
        "limit": 1,
        "offset": 1,
        "filter": { "grade": "A", "state": "NJ" }
      }""")
      val req = FakeRequest(POST, "/api/loans").withJsonBody(reqBody)
      val actual = call(controller.search(), req)

      val expectedEntities = Seq(
        LoanStat(
          id=126371281,
          loanAmount=Some(11200),
          date=Some("Dec-2017"),
          state=Some("NJ"),
          grade=Some("A"),
          subGrade=Some("A2"),
          ficoRangeLow=Some(795),
          ficoRangeHigh=Some(799),
        ),
      )
      val expected = PaginatedResult(totalCount = 914, expectedEntities, hasNextPage = true)

      status(actual) mustBe OK
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }
  }

  "LoanController aggregate" should {
    "return the min, max, and mean loan amounts for each state" in {
      val controller = new LoanController(stubControllerComponents())
      val action = controller.aggregate("loanAmount", "state")
      val req = FakeRequest(GET, "/api/loans/aggregate")
      val actual = call(action, req)

      val expected = Seq(BasicLoanStatsResult(
        grouping = "AK",
        min = Some(1500),
        max = Some(40000),
        mean = Some(16852.53036437247),
      ))

      status(actual) mustBe OK
      // Only check the first result because there are 50 states
      contentAsJson(actual).head mustBe Json.toJson(expected).head
    }

    "return the min, max, and mean loan amounts for each grade" in {
      val controller = new LoanController(stubControllerComponents())
      val action = controller.aggregate("loanAmount", "grade")
      val req = FakeRequest(GET, "/api/loans/aggregate")
      val actual = call(action, req)

      val expected = Seq(
        BasicLoanStatsResult(
          grouping = "A",
          min = Some(1000),
          max = Some(40000),
          mean = Some(14514.315422885573),
        ),
        BasicLoanStatsResult(
          grouping = "B",
          min = Some(1000),
          max = Some(40000),
          mean = Some(15074.927365048332),
        ),
        BasicLoanStatsResult(
          grouping = "C",
          min = Some(1000),
          max = Some(40000),
          mean = Some(15561.465724686863),
        ),
        BasicLoanStatsResult(
          grouping = "D",
          min = Some(1000),
          max = Some(40000),
          mean = Some(15575.28827315981),
        ),
        BasicLoanStatsResult(
          grouping = "E",
          min = Some(1000),
          max = Some(40000),
          mean = Some(16858.481669845307),
        ),
        BasicLoanStatsResult(
          grouping = "F",
          min = Some(1200),
          max = Some(40000),
          mean = Some(20251.72981878089),
        ),
        BasicLoanStatsResult(
          grouping = "G",
          min = Some(2575),
          max = Some(40000),
          mean = Some(22319.373673036094),
        ),
      )

      status(actual) mustBe OK
      contentAsJson(actual) mustBe Json.toJson(expected)
    }
  }
}
