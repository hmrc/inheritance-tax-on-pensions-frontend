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
import pages.NameOfDeceasedPage
import play.api.inject.bind
import views.html.NameOfDeceasedView
import base.SpecBase
import models._
import play.api.data.Form
import org.mockito.ArgumentMatchers._
import play.api.test.Helpers._
import org.mockito.Mockito.when
import forms.NameOfDeceasedFormProvider
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class NameOfDeceasedControllerSpec extends SpecBase {

  val formProvider = new NameOfDeceasedFormProvider()
  val form: Form[NameOfDeceased] = formProvider()

  lazy val nameOfDeceasedPageRoute: String = routes.NameOfDeceasedController.onPageLoad(srn, NormalMode).url

  "NameOfDeceasedController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, nameOfDeceasedPageRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NameOfDeceasedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(
          NameOfDeceasedPage,
          NameOfDeceased(
            title = Some("Mr"),
            firstForename = "John",
            secondForename = Some("William"),
            surname = "Doe"
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, nameOfDeceasedPageRoute)

        val view = application.injector.instanceOf[NameOfDeceasedView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(
            NameOfDeceased(
              title = Some("Mr"),
              firstForename = "John",
              secondForename = Some("William"),
              surname = "Doe"
            )
          ),
          srn,
          NormalMode
        )(using request, messages(application)).toString
      }
    }

    "must redirect to NINO page when valid data is submitted" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, nameOfDeceasedPageRoute)
            .withFormUrlEncodedBody(
              "title" -> "Mr",
              "firstForename" -> "John",
              "secondForename" -> "William",
              "surname" -> "Doe"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NinoOrReasonController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.NameOfDeceasedController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(
              "title" -> "Mr",
              "firstForename" -> "John",
              "secondForename" -> "William",
              "surname" -> "Doe"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, nameOfDeceasedPageRoute)
            .withFormUrlEncodedBody(
              "title" -> "",
              "firstForename" -> "",
              "secondForename" -> "",
              "surname" -> ""
            )

        val boundForm = form.bind(
          Map(
            "title" -> "",
            "firstForename" -> "",
            "secondForename" -> "",
            "surname" -> ""
          )
        )

        val view = application.injector.instanceOf[NameOfDeceasedView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

  }
}
