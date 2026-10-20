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
import connectors.InheritanceTaxOnPensionsConnector
import org.jsoup.Jsoup
import play.api.inject.bind
import base.SpecBase
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

class ViewPaidSubmissionReportControllerSpec extends SpecBase {

  private def onPageLoadUrl = routes.ViewPaidSubmissionReportController.onPageLoad(srn).url
  private def onViewPaidUrl =
    routes.ViewPaidSubmissionReportController.onViewPaid(srn, "119000004362").url

  "ViewPaidSubmissionReportController" - {
    Seq(true, false).foreach { isPsa =>
      s"must return OK and render the report in read-only mode for isPsa=$isPsa" in {
        val application =
          applicationBuilder(userAnswers = Some(prIndividualUserAnswers), isPsa = isPsa, usesSession = true).build()
        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          document.select("h1").text() mustBe "View report details"
          document.select("h1").hasClass("govuk-heading-l") mustBe true
          document.select(".govuk-inset-text").text() must include("This report is paid.")
          document.select(".govuk-inset-text").text() must include("If you need to change this report, you must")
        }
      }

      s"must display the report answers in the CYA layout for isPsa=$isPsa" in {
        val application =
          applicationBuilder(userAnswers = Some(prIndividualUserAnswers), isPsa = isPsa, usesSession = true).build()
        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          val summaryLists = document.select(".govuk-summary-list")
          summaryLists.size() mustBe 3

          val keys = summaryLists.select(".govuk-summary-list__key").eachText()
          keys must contain("Inheritance Tax reference number")
          keys must contain("Full name of deceased")
          keys must contain("Full name of PR")

          val values = summaryLists.select(".govuk-summary-list__value").eachText()
          values must contain("F123456/25A")
          values must contain("dec name")
        }
      }

      s"must not render any change or remove links for isPsa=$isPsa" in {
        val application =
          applicationBuilder(userAnswers = Some(prIndividualUserAnswers), isPsa = isPsa, usesSession = true).build()
        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          document.select(".govuk-summary-list__actions").isEmpty mustBe true
          document.select("a").eachText() must not contain "Change"
          document.select("form").isEmpty mustBe true
          document.select("button").isEmpty mustBe true
        }
      }

      s"must include contact HMRC and return to overview links for isPsa=$isPsa" in {
        val application =
          applicationBuilder(userAnswers = Some(prIndividualUserAnswers), isPsa = isPsa, usesSession = true).build()
        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          val contactLink = document.select(".govuk-inset-text #contact-hmrc")
          contactLink.size() mustBe 1
          contactLink.attr("href") must include("/you-must-contact-hmrc")
          contactLink.text() mustBe "contact HMRC"

          val returnLink = document.select("#return-to-overview")
          returnLink.size() mustBe 1
          returnLink.attr("href") mustBe routes.SubmissionListController.onPageLoad(srn).url
          returnLink.text() mustBe s"Return to the overview page for $schemeName"
        }
      }
    }

    "must redirect to journey recovery when user answers are missing" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()
      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "onViewPaid" - {
    Seq(true, false).foreach { isPsa =>
      s"must retrieve the report and redirect to the view page for isPsa=$isPsa" in {
        val connector = mock[InheritanceTaxOnPensionsConnector]
        when(connector.getReportByFbNumber(any(), any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(HttpResponse(OK, headers = Map("uuid" -> Seq(testUuid))))))

        val application = applicationBuilder(userAnswers = None, isPsa = isPsa, usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(connector))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, onViewPaidUrl)).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe onPageLoadUrl
        }
      }
    }

    "must redirect to journey recovery when the report cannot be retrieved" in {
      val connector = mock[InheritanceTaxOnPensionsConnector]
      when(connector.getReportByFbNumber(any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR))))

      val application = applicationBuilder(userAnswers = None, usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(connector))
        .build()

      running(application) {
        val result = route(application, FakeRequest(GET, onViewPaidUrl)).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
