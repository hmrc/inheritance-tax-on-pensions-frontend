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
import pages.LprTypePage
import play.api.inject.bind
import views.html.LprTypeView
import base.SpecBase
import models.{CheckMode, LprType, NormalMode}
import play.api.data.Form
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import forms.LprTypeFormProvider
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class LprTypeControllerSpec extends SpecBase {

  val formProvider = new LprTypeFormProvider()
  val form: Form[LprType] = formProvider()

  lazy val lprTypeRoute: String = routes.LprTypeController.onPageLoad(srn, NormalMode).url

  "LprTypeController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, lprTypeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LprTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(LprTypePage, LprType.Individual).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, lprTypeRoute)

        val view = application.injector.instanceOf[LprTypeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(LprType.Individual), srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Check Your Answers when valid data is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, lprTypeRoute)
            .withFormUrlEncodedBody(("value", LprType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.LprTypeController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", LprType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, lprTypeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[LprTypeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, lprTypeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, lprTypeRoute)
            .withFormUrlEncodedBody(("value", LprType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
