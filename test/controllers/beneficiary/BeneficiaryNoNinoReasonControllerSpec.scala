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
import views.html.beneficiary.BeneficiaryNoNinoReasonView
import base.SpecBase
import forms.beneficiary.BeneficiaryNoNinoReasonFormProvider
import play.api.inject
import controllers.beneficiary.routes
import models._
import pages.beneficiary._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class BeneficiaryNoNinoReasonControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new BeneficiaryNoNinoReasonFormProvider()
  private val form = formProvider()

  private val nameOfBeneficiary: IndividualName = individualName
  private val beneficiaryName: String = s"${nameOfBeneficiary.firstForename} ${nameOfBeneficiary.surname}"
  private val userAnswersWithBeneficiaryName: UserAnswers = emptyUserAnswers
    .set(BeneficiaryNamePage(0, JourneyRole.BeneficiaryIndividual), nameOfBeneficiary)
    .success
    .value
  private lazy val beneficiaryNoNinoReasonRoute =
    routes.BeneficiaryNoNinoReasonController.onPageLoad(srn, testIndex, NormalMode).url

  "BeneficiaryNoNinoReason Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(Some(userAnswersWithBeneficiaryName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryNoNinoReasonRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BeneficiaryNoNinoReasonView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, testIndex, NormalMode, beneficiaryName)(using
          request,
          messages(application)
        ).toString
        contentAsString(result) must include(messages(application)("site.saveAndContinue"))
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithBeneficiaryName
        .set(BeneficiaryNoNinoReasonPage(testIndex), testReasonNoNino)
        .success
        .value

      val application = applicationBuilder(Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryNoNinoReasonRoute)

        val view = application.injector.instanceOf[BeneficiaryNoNinoReasonView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(testReasonNoNino),
          srn,
          testIndex,
          NormalMode,
          beneficiaryName
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    Seq(
      (NormalMode, None, routes.BeneficiaryAddressController.start(srn, testIndex, NormalMode)),
      (CheckMode, None, routes.BeneficiaryAddressController.start(srn, testIndex, CheckMode)),
      (CheckMode, Some(BigDecimal("12.34")), controllers.routes.CheckYourAnswersController.onPageLoad(srn))
    ).foreach { case (mode, savedAmount, nextPage) =>
      s"must save and follow the next page in $mode with amountAlreadyEntered=${savedAmount.isDefined}" in {
        val answers = savedAmount.fold(userAnswersWithBeneficiaryName) { amount =>
          userAnswersWithBeneficiaryName
            .set(BeneficiaryIhtPayablePage(testIndex), amount)
            .success
            .value
            .set(BeneficiaryAddressPage(testIndex), testPrAddress)
            .success
            .value
        }
        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(answers)))
        val application = applicationBuilder(Some(answers), usesSession = true)
          .overrides(inject.bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.BeneficiaryNoNinoReasonController.onSubmit(srn, testIndex, mode).url)
            .withFormUrlEncodedBody("beneficiaryNoNinoReason" -> testReasonNoNino)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual nextPage.url
          verify(mockConnector).setUserAnswers(
            eqTo(answers.set(BeneficiaryNoNinoReasonPage(testIndex), testReasonNoNino).success.value),
            any(),
            any(),
            any(),
            any()
          )(using any())
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(Some(userAnswersWithBeneficiaryName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, beneficiaryNoNinoReasonRoute)
            .withFormUrlEncodedBody(("beneficiaryNoNinoReason", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BeneficiaryNoNinoReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, testIndex, NormalMode, beneficiaryName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryNoNinoReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, beneficiaryNoNinoReasonRoute)
            .withFormUrlEncodedBody(("beneficiaryNoNinoReason", testReasonNoNino))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the beneficiary name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryNoNinoReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the beneficiary name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, beneficiaryNoNinoReasonRoute)
            .withFormUrlEncodedBody(("beneficiaryNoNinoReason", testReasonNoNino))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
