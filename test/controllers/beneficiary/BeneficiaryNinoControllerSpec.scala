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
import views.html.beneficiary.BeneficiaryNinoView
import base.SpecBase
import forms.beneficiary.BeneficiaryNinoFormProvider
import controllers.beneficiary.routes
import models._
import pages.beneficiary.{BeneficiaryNinoPage, BeneficiaryNamePage}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when
import play.api.inject
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class BeneficiaryNinoControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new BeneficiaryNinoFormProvider()
  private val form = formProvider()
  private val nameOfBeneficiary: IndividualName = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Surname"
  )
  private val beneficiaryName: String = s"${nameOfBeneficiary.firstForename} ${nameOfBeneficiary.surname}"
  private val userAnswersWithBeneficiaryName: UserAnswers = emptyUserAnswers
    .set(BeneficiaryNamePage(0, JourneyRole.BeneficiaryIndividual), nameOfBeneficiary)
    .success
    .value
  private lazy val ninoRoute = routes.BeneficiaryNinoController.onPageLoad(srn, 0, NormalMode).url

  "BeneficiaryNinoPage Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaryName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BeneficiaryNinoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, beneficiaryName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET" in {

      val userAnswers = userAnswersWithBeneficiaryName
        .set(BeneficiaryNinoPage(0), "AA123456A")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val view = application.injector.instanceOf[BeneficiaryNinoView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Nino("AA123456A")), srn, NormalMode, beneficiaryName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithBeneficiaryName)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaryName), usesSession = true)
        .overrides(
          inject.bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "AA123456A"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BeneficiaryListController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaryName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BeneficiaryNinoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, beneficiaryName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
