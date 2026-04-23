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
import pages.{NameOfDeceasedPage, NinoOrReasonPage}
import play.api.inject.bind
import views.html.NinoOrReasonView
import base.SpecBase
import models._
import play.api.data.Form
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import forms.{NinoOrReasonFormData, NinoOrReasonFormProvider}
import uk.gov.hmrc.http.HttpResponse
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class NinoOrReasonControllerSpec extends SpecBase with MockitoSugar {

  lazy val ninoOrReasonRoute: String = routes.NinoOrReasonController.onPageLoad(srn, NormalMode).url

  val formProvider = new NinoOrReasonFormProvider()
  val form: Form[NinoOrReasonFormData] = formProvider()
  val nameOfDeceased: NameOfDeceased = NameOfDeceased(
    title = Some("Mr"),
    firstForename = "John",
    secondForename = Some("William"),
    surname = "Doe"
  )
  val deceasedName: String = s"${nameOfDeceased.firstForename} ${nameOfDeceased.surname}"
  val userAnswersWithDeceasedName: UserAnswers = emptyUserAnswers
    .set(NameOfDeceasedPage, nameOfDeceased)
    .success
    .value
  val validNino: String = ninoGen.sample.value
  val validNinoWithSpacesAndLowercase: String = validNino.toLowerCase.grouped(2).mkString(" ")

  "NinoOrReason Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoOrReasonRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NinoOrReasonView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithDeceasedName
        .set(NinoOrReasonPage, NinoOrReasonFormData(NinoOrReason.values.head, Some(validNino), None))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoOrReasonRoute)

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(NinoOrReasonFormData(NinoOrReason.values.head, Some(validNino), None)),
          srn,
          NormalMode,
          deceasedName
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when yes is selected and the National Insurance number is empty" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> ""
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.Yes, None, None))
          .withError("nino", "ninoOrReason.nino.error.required")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when yes is selected and the National Insurance number is not in the correct format" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> "NW12345C"
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.Yes, Some("NW12345C"), None))
          .withError("nino", "ninoOrReason.nino.error.invalid")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when yes is selected and the National Insurance number contains invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> "NW12-3456C"
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.Yes, Some("NW12-3456C"), None))
          .withError("nino", "ninoOrReason.nino.error.invalid")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must accept a lowercase National Insurance number with spaces by formatting it" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> validNinoWithSpacesAndLowercase
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BirthDeathDatesController.onPageLoad(srn, NormalMode).url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.NinoOrReasonController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> validNino
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must clear a previously entered reason when yes is submitted" in {

      val existingAnswers = userAnswersWithDeceasedName
        .set(NinoOrReasonPage, NinoOrReasonFormData(NinoOrReason.No, None, Some("Existing reason")))
        .success
        .value

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(existingAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.Yes.toString,
              "nino" -> validNino,
              "reasonForNoNino" -> "Existing reason"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        val savedAnswer = (userAnswersCaptor.getValue.data \ "ninoOrReason").as[NinoOrReasonFormData]
        savedAnswer mustEqual NinoOrReasonFormData(NinoOrReason.Yes, Some(validNino), None)
      }
    }

    "must clear a previously entered nino when no is submitted" in {

      val existingAnswers = userAnswersWithDeceasedName
        .set(NinoOrReasonPage, NinoOrReasonFormData(NinoOrReason.Yes, Some(validNino), None))
        .success
        .value

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(existingAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.No.toString,
              "nino" -> validNino,
              "reasonForNoNino" -> "The deceased was not a UK citizen"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        val savedAnswer = (userAnswersCaptor.getValue.data \ "ninoOrReason").as[NinoOrReasonFormData]
        savedAnswer mustEqual NinoOrReasonFormData(
          NinoOrReason.No,
          None,
          Some("The deceased was not a UK citizen")
        )
      }
    }

    "must return a Bad Request when no is selected and the reason is empty" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.No.toString,
              "reasonForNoNino" -> ""
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.No, None, None))
          .withError("reasonForNoNino", "ninoOrReason.reasonForNoNino.error.required")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when no is selected and the reason is longer than 160 characters" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val reason = "a" * 161
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.No.toString,
              "reasonForNoNino" -> reason
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.No, None, Some(reason)))
          .withError("reasonForNoNino", "ninoOrReason.reasonForNoNino.error.length")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when no is selected and the reason contains a new line" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val reason = "first line\nsecond line"
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(
              "value" -> NinoOrReason.No.toString,
              "reasonForNoNino" -> reason
            )

        val boundForm = form
          .fill(NinoOrReasonFormData(NinoOrReason.No, None, Some(reason)))
          .withError("reasonForNoNino", "ninoOrReason.reasonForNoNino.error.invalid")

        val view = application.injector.instanceOf[NinoOrReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoOrReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoOrReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(("value", NinoOrReason.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoOrReasonRoute)
            .withFormUrlEncodedBody(("value", NinoOrReason.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
