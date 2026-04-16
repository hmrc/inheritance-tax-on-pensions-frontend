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
import views.html.PspDeclarationView
import base.SpecBase
import models.IhtpReportSubmissionResponse
import play.api.data.Form
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._
import forms.PspDeclarationFormProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

import java.time.Instant

class PspDeclarationControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new PspDeclarationFormProvider()
  val form: Form[String] = formProvider(defaultSchemeDetails.authorisingPSAID)
  lazy val pspDeclarationRoute: String = routes.PspDeclarationController.onPageLoad(srn).url

  "PspDeclaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, pspDeclarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PspDeclarationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, schemeName)(using request, messages(application)).toString
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
        val request =
          FakeRequest(POST, pspDeclarationRoute)
            .withFormUrlEncodedBody(("value", "A1234567"))

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
        val request =
          FakeRequest(POST, pspDeclarationRoute)
            .withFormUrlEncodedBody(("value", "A1234567"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, pspDeclarationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PspDeclarationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, schemeName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when authorisingPSAID not matched" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, pspDeclarationRoute)
            .withFormUrlEncodedBody(("value", "A1234557"))

        val boundForm = form.bind(Map("value" -> "A1234557"))

        val view = application.injector.instanceOf[PspDeclarationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, schemeName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, pspDeclarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, pspDeclarationRoute)
            .withFormUrlEncodedBody(("value", "A1234567"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
