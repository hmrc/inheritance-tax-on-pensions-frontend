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
import pages.IndividualNamePage
import play.api.inject.bind
import views.html.IndividualNameView
import base.SpecBase
import models._
import org.mockito.ArgumentMatchers._
import play.api.test.Helpers._
import org.mockito.Mockito.when
import forms.IndividualNameFormProvider
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IndividualNameControllerSpec extends SpecBase {

  private val formProvider = new IndividualNameFormProvider()

  private val individualName = IndividualName(
    title = Some("Mr"),
    firstForename = "John",
    secondForename = Some("William"),
    surname = "Doe"
  )

  private case class JourneyRoleTestCase(
    journeyRole: JourneyRole,
    nextPageUrl: String
  )

  private lazy val journeyRoleTestCases = Seq(
    JourneyRoleTestCase(
      JourneyRole.Deceased,
      routes.NinoOrReasonController.onPageLoad(srn, NormalMode).url
    ),
    JourneyRoleTestCase(
      JourneyRole.LprIndividual,
      routes.CheckYourAnswersController.onPageLoad(srn).url
    )
  )

  "IndividualNameController Controller" - {

    journeyRoleTestCases.foreach { testCase =>
      val journeyRole = testCase.journeyRole

      s"must return OK and the correct view for a GET for ${journeyRole.key}" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(GET, "/test-only/individual-name")
          val controller = application.injector.instanceOf[IndividualNameController]

          val result = controller.onPageLoad(srn, NormalMode, journeyRole)(request)

          val view = application.injector.instanceOf[IndividualNameView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formProvider(journeyRole), srn, NormalMode, journeyRole)(using
            request,
            messages(application)
          ).toString
        }
      }

      s"must populate the view correctly on a GET when ${journeyRole.key} has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IndividualNamePage(journeyRole), individualName)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(GET, "/test-only/individual-name")
          val controller = application.injector.instanceOf[IndividualNameController]

          val view = application.injector.instanceOf[IndividualNameView]

          val result = controller.onPageLoad(srn, NormalMode, journeyRole)(request)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formProvider(journeyRole).fill(individualName),
            srn,
            NormalMode,
            journeyRole
          )(using request, messages(application)).toString
        }
      }

      s"must redirect to the correct next page when valid ${journeyRole.key} data is submitted" in {

        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(mock[HttpResponse]))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val controller = application.injector.instanceOf[IndividualNameController]
          val request =
            FakeRequest(POST, "/test-only/individual-name")
              .withFormUrlEncodedBody(validFormData*)

          val result = controller.onSubmit(srn, NormalMode, journeyRole)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must endWith(testCase.nextPageUrl)
        }
      }

      s"must redirect to Check Your Answers when valid ${journeyRole.key} data is submitted in CheckMode" in {

        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(mock[HttpResponse]))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.IndividualNameController.onSubmit(srn, CheckMode, journeyRole).url)
              .withFormUrlEncodedBody(validFormData*)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
        }
      }

      s"must return a Bad Request and errors when invalid ${journeyRole.key} data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

        running(application) {
          val controller = application.injector.instanceOf[IndividualNameController]
          val request =
            FakeRequest(POST, "/test-only/individual-name")
              .withFormUrlEncodedBody(invalidFormData*)

          val boundForm = formProvider(journeyRole).bind(invalidFormData.toMap)

          val view = application.injector.instanceOf[IndividualNameView]

          val result = controller.onSubmit(srn, NormalMode, journeyRole)(request)

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, srn, NormalMode, journeyRole)(using
            request,
            messages(application)
          ).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET when the journey role is unknown" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, "/test-only/unknown-name-page")

        val controller = application.injector.instanceOf[IndividualNameController]

        val result = controller.onPageLoad(srn, NormalMode, JourneyRole.Unknown)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when the journey role is unknown" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, "/test-only/unknown-name-page")
            .withFormUrlEncodedBody(validFormData*)

        val controller = application.injector.instanceOf[IndividualNameController]

        val result = controller.onSubmit(srn, NormalMode, JourneyRole.Unknown)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for an unsupported next page state" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val controller = application.injector.instanceOf[IndividualNameController]

        controller.nextPage(srn, NormalMode, JourneyRole.Unknown) mustEqual routes.JourneyRecoveryController
          .onPageLoad()
      }
    }

  }

  private val validFormData = Seq(
    "title" -> "Mr",
    "firstForename" -> "John",
    "secondForename" -> "William",
    "surname" -> "Doe"
  )

  private val invalidFormData = Seq(
    "title" -> "",
    "firstForename" -> "",
    "secondForename" -> "",
    "surname" -> ""
  )
}
