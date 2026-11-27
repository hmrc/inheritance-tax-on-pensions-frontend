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
import pages._
import play.api.inject.bind
import views.html.WhoRepresentsEstateView
import base.SpecBase
import forms.WhoRepresentsEstateFormProvider
import models._
import play.api.data.Form
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.Future

class WhoRepresentsEstateControllerSpec extends SpecBase {

  val formProvider = new WhoRepresentsEstateFormProvider()
  val form: Form[RepresentativeType] = formProvider()
  lazy val whoRepresentsEstateRoute: String = routes.WhoRepresentsEstateController.onPageLoad(srn, NormalMode).url

  "WhoRepresentsEstateController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, whoRepresentsEstateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhoRepresentsEstateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, None)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(WhoRepresentsEstatePage, RepresentativeType.PR).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, whoRepresentsEstateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhoRepresentsEstateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(RepresentativeType.PR), srn, NormalMode, None)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must display the deceased's name in the heading" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Peter",
            secondForename = None,
            surname = "Smith"
          )
        )
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, whoRepresentsEstateRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(
          "Who is representing the estate of Peter Smith?"
        )
      }
    }

    "must save PR and redirect to the PR type page when PR is submitted" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(
            bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whoRepresentsEstateRoute)
            .withFormUrlEncodedBody(("value", RepresentativeType.PR.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.PrTypeController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        userAnswersCaptor.getValue.get(WhoRepresentsEstatePage) mustBe
          Some(RepresentativeType.PR)
      }
    }

    "must save PPR and redirect to the PR type page when PPR is submitted" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(
            bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whoRepresentsEstateRoute)
            .withFormUrlEncodedBody(("value", RepresentativeType.PPR.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.PrTypeController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        userAnswersCaptor.getValue.get(WhoRepresentsEstatePage) mustBe
          Some(RepresentativeType.PPR)
      }
    }

    "must redirect to the PR type page in CheckMode when PR type is missing" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.WhoRepresentsEstateController.onSubmit(srn, CheckMode).url)
          .withFormUrlEncodedBody(("value", RepresentativeType.PR.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.PrTypeController.onPageLoad(srn, CheckMode).url
      }
    }

    "must redirect to Check Your Answers in CheckMode when PR type is present" in {

      val userAnswers = emptyUserAnswers
        .set(PrTypePage, PrType.Individual)
        .success
        .value

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.WhoRepresentsEstateController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", RepresentativeType.PPR.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, whoRepresentsEstateRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhoRepresentsEstateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, None)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, whoRepresentsEstateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, whoRepresentsEstateRoute)
            .withFormUrlEncodedBody(("value", RepresentativeType.PR.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
