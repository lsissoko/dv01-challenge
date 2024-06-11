package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import persistence.LoanStatsTable.LoanStat

class LoanControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

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
}
