/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import play.api.test.FakeRequest
import services.SubmissionListService
import org.jsoup.Jsoup
import play.api.inject.bind
import base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import models._
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

import java.time.{Instant, LocalDate}

class PaidReportsControllerSpec extends SpecBase {
  private val overviewReport = IhtpOverviewReport(
    uuid = None,
    fbNumber = Some("119000004320"),
    submissionDate = Some(Instant.parse("2026-04-10T16:12:49Z")),
    paymentDueDate = Some(LocalDate.of(2026, 2, 2)),
    ihtVersion = "001",
    inheritanceTaxReference = "A123456/25A",
    paymentReference = Some("A123456/25A629671"),
    title = Some("Dr"),
    firstForename = Some("Firstname"),
    secondForename = Some("M"),
    surname = Some("Surname"),
    nino = None,
    ihtpStatus = "Not reconciled"
  )

  private val paidReport = overviewReport.copy(ihtpStatus = "Paid", uuid = Some(testUuid))

  private def builder(result: Either[UpstreamErrorResponse, IhtpOverviewResponse], isPsa: Boolean = true) = {
    val service = mock[SubmissionListService]
    when(service.getSubmissionList()(using any(), any())).thenReturn(Future.successful(result))
    applicationBuilder(userAnswers = None, isPsa = isPsa, usesSession = true)
      .overrides(bind[SubmissionListService].toInstance(service))
  }

  private def response(reports: Seq[IhtpOverviewReport]) =
    Right(IhtpOverviewResponse(IhtpOverviewSuccess(reports)))

  "Paid reports" - {
    Seq(true, false).foreach { isPsa =>
      s"must show only paid reports for isPsa=$isPsa with a dummy search bar and no amendment navigation" in {
        val application = builder(response(Seq(overviewReport, paidReport)), isPsa).build()
        running(application) {
          val result = route(application, FakeRequest(GET, routes.PaidReportsController.onPageLoad(srn).url)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.select("h1").text() mustBe "Paid Inheritance Tax on a pension reports"
          document.select("h1").hasClass("govuk-heading-l") mustBe true
          document.select("h1").hasClass("govuk-heading-xl") mustBe false
          document.select("#scheme-name-caption").first().ownText() mustBe schemeName
          document.select("#scheme-name-caption").hasClass("govuk-caption-l") mustBe true
          document.select("tbody tr").size() mustBe 1
          document.select("tbody td").first().text() mustBe "Paid"
          document.select("tbody th a").text() must include(paidReport.deceasedName)
          document.select("tbody th a").attr("href") mustBe "#"
          document.select("#find-report").size() mustBe 1
          document.select("label[for=find-report]").text() mustBe messages(application)(
            "submissionList.find.input.label"
          )
          document.select("fieldset button[type=button]").text() mustBe "Search"
          document.select("#find-report").first().closest("form") mustBe null
          document.select("#return-to-active-reports").attr("href") mustBe routes.SubmissionListController
            .onPageLoad(srn)
            .url
          document.text() must include("Showing 1 - 1 of 1 reports")
        }
      }
    }

    Seq(
      response(Seq.empty),
      response(Seq(overviewReport)),
      Left(UpstreamErrorResponse("No records", UNPROCESSABLE_ENTITY))
    ).zipWithIndex.foreach { (data, index) =>
      s"must show the empty state without a table or pagination ($index)" in {
        val application = builder(data).build()
        running(application) {
          val result = route(application, FakeRequest(GET, routes.PaidReportsController.onPageLoad(srn).url)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.text() must include("This scheme does not currently have any paid reports.")
          document.select("h1").hasClass("govuk-heading-l") mustBe true
          document.select("h1").hasClass("govuk-heading-xl") mustBe false
          document.select("#find-report").size() mustBe 1
          document.select("fieldset button[type=button]").text() mustBe "Search"
          document.select("table").isEmpty mustBe true
          document.select(".govuk-pagination").isEmpty mustBe true
          document.select("#return-to-active-reports").size() mustBe 1
        }
      }
    }

    "must filter before pagination and link to paid-report pages" in {
      val reports =
        (1 to 16).flatMap(index => Seq(overviewReport, paidReport.copy(firstForename = Some(s"Person$index"))))
      val application = builder(response(reports)).build()
      running(application) {
        val url = routes.PaidReportsController.onPageLoad(srn).url
        val result = route(application, FakeRequest(GET, s"$url?page=2")).value
        status(result) mustBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.select("tbody tr").size() mustBe 1
        document.select("tbody").text() must include("Person16")
        document.text() must include("Showing 16 - 16 of 16 reports")
        document.select(".govuk-pagination a").eachAttr("href").forEach(href => href must startWith(url))
      }
    }

    "must render reports with no due date or payment reference" in {
      val application = builder(response(Seq(paidReport.copy(paymentDueDate = None, paymentReference = None)))).build()
      running(application) {
        status(route(application, FakeRequest(GET, routes.PaidReportsController.onPageLoad(srn).url)).value) mustBe OK
      }
    }

    "must redirect retrieval failures to journey recovery" in {
      val application = builder(Left(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR))).build()
      running(application) {
        val result = route(application, FakeRequest(GET, routes.PaidReportsController.onPageLoad(srn).url)).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
