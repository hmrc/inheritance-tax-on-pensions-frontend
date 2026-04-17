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
import services.ReportSubmissionService
import play.api.inject.bind
import views.html.PsaDeclarationView
import base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import models.IhtpReportSubmissionResponse
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._

import scala.concurrent.Future

import java.time.Instant

class PsaDeclarationControllerSpec extends SpecBase with MockitoSugar {

  lazy val psaDeclarationRoute: String = routes.PsaDeclarationController.onPageLoad(srn).url

  "PsaDeclaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, psaDeclarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PsaDeclarationView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(srn, schemeName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to ConfirmationController when submission is successful" in {
      val mockReportSubmissionService = mock[ReportSubmissionService]
      val response = IhtpReportSubmissionResponse(Instant.now(), "formBundle", "paymentRef")
      when(mockReportSubmissionService.submitReport(any())(using any(), any()))
        .thenReturn(Future.successful(Right(response)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReportSubmissionService].toInstance(mockReportSubmissionService))
        .build()

      running(application) {
        val request = FakeRequest(POST, psaDeclarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ConfirmationController.onPageLoad(srn).url
      }
    }

    "must redirect to JourneyRecoveryController when submission fails" in {
      val mockReportSubmissionService = mock[ReportSubmissionService]
      val errorResponse = UpstreamErrorResponse("Submission failed", 500)
      when(mockReportSubmissionService.submitReport(any())(using any(), any()))
        .thenReturn(Future.successful(Left(errorResponse)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReportSubmissionService].toInstance(mockReportSubmissionService))
        .build()

      running(application) {
        val request = FakeRequest(POST, psaDeclarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
