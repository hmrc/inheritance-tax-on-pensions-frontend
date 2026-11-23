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
import services.UserAnswersService
import play.api.inject.bind
import views.html.beneficiary.ChangeBeneficiaryAddressView
import base.SpecBase
import forms.beneficiary.BeneficiaryAddressFormProvider
import models.beneficiary.BeneficiaryType
import models._
import pages.beneficiary._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class ChangeBeneficiaryAddressControllerSpec extends SpecBase {

  private val formProvider = new BeneficiaryAddressFormProvider()
  private def answers(role: JourneyRole): UserAnswers = (for {
    first <- individualBeneficiaryUserAnswers.set(BeneficiaryAddressPage(0), testPrAddress)
    typed <- first.set(
      BeneficiaryTypePage(1),
      role match {
        case JourneyRole.BeneficiaryTrust => BeneficiaryType.Trust
        case _ => BeneficiaryType.Individual
      }
    )
    contact <- typed.set(BeneficiaryNamePage(1, role), individualName)
    named <-
      if (role == JourneyRole.BeneficiaryTrust) contact.set(BeneficiaryTrustNamePage(1), trustName)
      else contact.set(BeneficiaryHasNinoPage(1), true).flatMap(_.set(BeneficiaryNinoPage(1), testNino))
    address <- named.set(BeneficiaryAddressPage(1), testPrAddress)
  } yield address).success.value

  private val roleCases = Seq(
    (JourneyRole.BeneficiaryIndividual, "Firstname Lastname"),
    (JourneyRole.BeneficiaryTrust, "Testdata Trust")
  )

  "ChangeBeneficiaryAddressController" - {

    roleCases.foreach { case (journeyRole, displayName) =>
      s"must show the existing address for ${journeyRole.name}" in {
        val application = applicationBuilder(userAnswers = Some(answers(journeyRole)), usesSession = true).build()

        running(application) {
          val request =
            FakeRequest(GET, routes.ChangeBeneficiaryAddressController.onPageLoad(srn, 1).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ChangeBeneficiaryAddressView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            formProvider(testPrAddress.country).fill(testPrAddress),
            srn,
            1,
            displayName,
            isUkAddress = true
          )(using request, messages(application)).toString
        }
      }

      s"must save the edited address, retain the country, and return to CYA for ${journeyRole.name}" in {
        val mockUserAnswersService = mock[UserAnswersService]
        when(mockUserAnswersService.set(any())(using any(), any()))
          .thenReturn(Future.successful(Right(emptyUserAnswers)))

        val application = applicationBuilder(userAnswers = Some(answers(journeyRole)), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.ChangeBeneficiaryAddressController.onSubmit(srn, testIndex).url)
              .withFormUrlEncodedBody(
                "addressLine1" -> "1 Line",
                "addressLine2" -> "",
                "addressLine3" -> "",
                "addressLine4" -> "City",
                "postCode" -> "NE2 2AA"
              )

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.CheckYourAnswersController
            .onPageLoad(srn)
            .url

          val answersCaptor: ArgumentCaptor[UserAnswers] =
            ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockUserAnswersService).set(answersCaptor.capture())(using any(), any())

          answersCaptor.getValue.get(BeneficiaryAddressPage(testIndex)) mustBe Some(
            PrAddress(
              addressLine1 = "1 Line",
              addressLine2 = None,
              addressLine3 = None,
              addressLine4 = "City",
              postCode = Some("NE2 2AA"),
              country = "GB"
            )
          )
        }
      }

      s"must return Bad Request for invalid data for ${journeyRole.name}" in {
        val mockUserAnswersService = mock[UserAnswersService]
        val application = applicationBuilder(userAnswers = Some(answers(journeyRole)), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.ChangeBeneficiaryAddressController.onSubmit(srn, testIndex).url)
              .withFormUrlEncodedBody(
                "addressLine1" -> "%",
                "addressLine2" -> "",
                "addressLine3" -> "",
                "addressLine4" -> "",
                "postCode" -> ""
              )

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
          verify(mockUserAnswersService, never).set(any())(using any(), any())
        }
      }
    }

    "must use the Postal code label for a non-UK address" in {
      val nonUkAddress = testPrAddress.copy(country = "FR", postCode = Some("75001"))
      val userAnswers =
        answers(JourneyRole.BeneficiaryIndividual).set(BeneficiaryAddressPage(testIndex), nonUkAddress).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangeBeneficiaryAddressController.onPageLoad(srn, testIndex).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) must include("<b>Postal code</b> (optional)")
        (contentAsString(result) must not).include("Postcode (optional)")
      }
    }

    "must redirect to Journey Recovery when the address is missing" in {
      val userAnswers = emptyUserAnswers
        .set(
          BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual),
          IndividualName(None, "Firstnamethree", None, "Surnametwo")
        )
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangeBeneficiaryAddressController.onPageLoad(srn, testIndex).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the trust name is missing" in {
      val userAnswers = answers(JourneyRole.BeneficiaryTrust).remove(BeneficiaryTrustNamePage(1)).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangeBeneficiaryAddressController.onPageLoad(srn, 1).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when user answers are missing" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangeBeneficiaryAddressController.onPageLoad(srn, testIndex).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
