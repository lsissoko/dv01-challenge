package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class LoanControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "LoanController GET" should {

    "return an empty list when the limit is negative" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(-1234).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq.empty[(Long, Int)]

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return an empty list when the limit is zero" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(0).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq.empty[(Long, Int)]

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }

    "return the first N loans from the database" in {
      val controller = new LoanController(stubControllerComponents())
      val actual = controller.find(2).apply(FakeRequest(GET, "/api/loans"))

      val expected = Seq(
        // Seq(id, loan_amnt)
        Seq(126285300, 40000),
        Seq(126367373, 24000)
      )

      status(actual) mustBe OK
      contentType(actual) mustBe Some("application/json")
      contentAsJson(actual) mustEqual Json.toJson(expected)
    }
  }
}
