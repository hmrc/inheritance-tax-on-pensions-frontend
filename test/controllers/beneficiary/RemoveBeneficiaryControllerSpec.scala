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

package controllers.beneficiary

import play.api.test.FakeRequest
import connectors.InheritanceTaxOnPensionsConnector
import play.api.inject.bind
import views.html.beneficiary.RemoveBeneficiaryView
import base.SpecBase
import forms.beneficiary.RemoveBeneficiaryFormProvider
import models.beneficiary.BeneficiaryType
import models.{JourneyRole, NormalMode, UserAnswers}
import pages.beneficiary._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._

import scala.concurrent.Future

class RemoveBeneficiaryControllerSpec extends SpecBase {

  private val form = new RemoveBeneficiaryFormProvider()()
  private lazy val routeUrl = routes.RemoveBeneficiaryController.onPageLoad(srn, testIndex).url
  private val answersWithBeneficiary = emptyUserAnswers
    .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual)
    .success
    .value
    .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), individualName)
    .success
    .value

  private val secondBeneficiaryName = individualName.copy(firstForename = "Jane", secondForename = None)

  private val answersWithTwoBeneficiaries = answersWithBeneficiary
    .set(BeneficiaryHasNinoPage(testIndex), true)
    .success
    .value
    .set(BeneficiaryTypePage(testIndex + 1), BeneficiaryType.Individual)
    .success
    .value
    .set(BeneficiaryNamePage(testIndex + 1, JourneyRole.BeneficiaryIndividual), secondBeneficiaryName)
    .success
    .value
    .set(BeneficiaryHasNinoPage(testIndex + 1), false)
    .success
    .value

  "RemoveBeneficiaryController" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[RemoveBeneficiaryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, testIndex, "John Doe")(using request, messages(application)).toString
      }
    }

    "must remove all data for the beneficiary and return to the beneficiary list when another remains" in {
      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))
      val application = applicationBuilder(userAnswers = Some(answersWithTwoBeneficiaries), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BeneficiaryListController.onPageLoad(srn).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        val savedAnswers = userAnswersCaptor.getValue
        savedAnswers.get(BeneficiariesPage()).value.beneficiaries.size mustEqual 1
        savedAnswers
          .get(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual))
          .value mustEqual secondBeneficiaryName
        savedAnswers.get(BeneficiaryHasNinoPage(testIndex)).value mustBe false
        savedAnswers.get(BeneficiaryTypePage(testIndex + 1)) mustBe None
        savedAnswers.get(BeneficiaryNamePage(testIndex + 1, JourneyRole.BeneficiaryIndividual)) mustBe None
        savedAnswers.get(BeneficiaryHasNinoPage(testIndex + 1)) mustBe None
      }
    }

    "must remove the last beneficiary and return to the beneficiaries known page" in {
      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())
        userAnswersCaptor.getValue.get(BeneficiariesPage()).value.beneficiaries mustBe empty
      }
    }

    "must keep the beneficiary and return to the beneficiary list when No is submitted" in {
      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "false")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BeneficiaryListController.onPageLoad(srn).url
        verify(mockConnector, never()).setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must return a Bad Request when no option is selected" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "")
        val result = route(application, request).value
        val view = application.injector.instanceOf[RemoveBeneficiaryView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(form.bind(Map("value" -> "")), srn, testIndex, "John Doe")(using request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when the beneficiary name is missing" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on submit when the beneficiary name is missing" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    testInvalidBeneficiaryIndexes.foreach { invalidIndex =>
      s"must return Not Found for invalid index $invalidIndex" in {
        val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

        running(application) {
          val result = route(
            application,
            FakeRequest(GET, routes.RemoveBeneficiaryController.onPageLoad(srn, invalidIndex).url)
          ).value

          status(result) mustEqual NOT_FOUND
        }
      }
    }
  }
}
