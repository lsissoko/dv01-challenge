package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import persistence.LoanStatsTable.LoanStat
import org.apache.pekko.actor.ActorSystem
import models.PaginatedResult

class LoanControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val totalCount = 118636

  implicit val actorSystem: ActorSystem = ActorSystem()

  "LoanController GET" should {

    "return an empty list when the limit is negative" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(-1234).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq.empty[LoanStat]

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return an empty list when the limit is zero" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(0).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq.empty[LoanStat]

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return the first N loans from the database" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(2).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq(
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
          id=126367373,
          loanAmount=Some(24000),
          date=Some("Dec-2017"),
          state=Some("IN"),
          grade=Some("C"),
          subGrade=Some("C4"),
          ficoRangeLow=Some(705),
          ficoRangeHigh=Some(709),
        ),
      )

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }
  }

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
}
