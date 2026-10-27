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
import pages.AreBeneficiariesKnownPage
import play.api.inject.bind
import views.html.CurrencyEntryView
import models._
import pages.beneficiary._
import viewmodels.CurrencyEntryPageViewModel
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import base.SpecBase
import models.currency.BeneficiaryIhtPayableEntry
import forms.CurrencyEntryFormProvider
import models.beneficiary.BeneficiaryType

import scala.concurrent.Future

class BeneficiaryIhtPayableControllerSpec extends SpecBase {
  private val secondBeneficiaryName = individualName.copy(firstForename = "Firstnametwo")

  private val answersIndividuls = (for {
    firstWithAmount <- individualBeneficiaryUserAnswers.set(BeneficiaryIhtPayablePage(0), BigDecimal("10.00"))
    second <- firstWithAmount.set(BeneficiaryTypePage(1), BeneficiaryType.Individual)
    namedSecond <- second.set(BeneficiaryNamePage(1, JourneyRole.BeneficiaryIndividual), secondBeneficiaryName)
    withoutNino <- namedSecond.set(BeneficiaryHasNinoPage(1), false)
    withReason <- withoutNino.set(BeneficiaryNoNinoReasonPage(1), testReasonNoNino)
    withAddress <- withReason.set(BeneficiaryAddressPage(1), testPrAddress)
  } yield withAddress).success.value

  private val answersTrust = (for {
    firstWithAmount <- individualBeneficiaryUserAnswers.set(BeneficiaryIhtPayablePage(0), BigDecimal("10.00"))
    second <- firstWithAmount.set(BeneficiaryTypePage(1), BeneficiaryType.Trust)
    namedSecond <- second.set(BeneficiaryNamePage(1, JourneyRole.BeneficiaryTrust), secondBeneficiaryName)
    trustname <- namedSecond.set(BeneficiaryTrustNamePage(1), trustName)
    withAddress <- trustname.set(BeneficiaryAddressPage(1), testPrAddress)
  } yield withAddress).success.value

  Seq((answersIndividuls, "individual"), (answersTrust, "trust")).foreach { (answers, testName) =>
    s"BeneficiaryIhtPayableController ${testName}" - {
      Seq(NormalMode, CheckMode).foreach { mode =>
        s"must populate the shared view with the selected beneficiary's saved amount in $mode" in {
          val savedAnswers = (for {
            hasNino <- answers.set(BeneficiaryHasNinoPage(1), true)
            withNino <- hasNino.set(BeneficiaryNinoPage(1), testNino)
            withAmount <- withNino.set(BeneficiaryIhtPayablePage(1), BigDecimal("1234.56"))
          } yield withAmount).success.value
          val app = applicationBuilder(Some(savedAnswers), usesSession = true).build()
          running(app) {
            val request = FakeRequest(GET, routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, mode).url)
            val result = route(app, request).value
            val view = app.injector.instanceOf[CurrencyEntryView]
            val entry = BeneficiaryIhtPayableEntry(1)
            val form = new CurrencyEntryFormProvider()(entry).fill(BigDecimal("1234.56"))
            val page = CurrencyEntryPageViewModel(entry, savedAnswers, srn, mode)(using messages(app))
            status(result) mustBe OK
            contentAsString(result) mustBe view(form, page)(using request, messages(app)).toString
          }
        }

        s"must save the maximum for only the selected beneficiary and follow the next page in $mode" in {
          val savedAnswers = answers.set(BeneficiaryIhtPayablePage(1), BigDecimal("999999999.99")).success.value
          val service = mock[UserAnswersService]
          when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(savedAnswers)))
          val app = applicationBuilder(Some(answers), usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val result = route(
              app,
              FakeRequest(POST, routes.BeneficiaryIhtPayableController.onSubmit(srn, 1, mode).url)
                .withFormUrlEncodedBody("value" -> "999,999,999.99")
            ).value
            status(result) mustBe SEE_OTHER
            val expectedPage = mode match {
              case NormalMode => routes.BeneficiaryInterestPayableController.onPageLoad(srn, 1, NormalMode)
              case CheckMode => routes.BeneficiaryInterestPayableController.onPageLoad(srn, 1, CheckMode)
            }
            redirectLocation(result).value mustBe expectedPage.url
            val captured = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(service).set(captured.capture())(using any(), any())
            captured.getValue mustBe savedAnswers
          }
        }
      }

      "must reject missing, invalid and over-limit amounts without saving" in {
        val service = mock[UserAnswersService]
        val app = applicationBuilder(Some(answers), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(service))
          .build()
        running(app) {
          Seq("", "12%", "1000000000").foreach { input =>
            val result = route(
              app,
              FakeRequest(POST, routes.BeneficiaryIhtPayableController.onSubmit(srn, 1, NormalMode).url)
                .withFormUrlEncodedBody("value" -> input)
            ).value
            status(result) mustBe BAD_REQUEST
          }
          verifyNoInteractions(service)
        }
      }

      "must block incomplete and unknown-beneficiary journeys on GET and POST" in {
        val invalidAnswers = Seq(
          answers.remove(BeneficiaryAddressPage(1)).success.value,
          answers
            .remove(
              testName match {
                case "individual" => BeneficiaryNamePage(1, JourneyRole.BeneficiaryIndividual)
                case _ => BeneficiaryNamePage(1, JourneyRole.BeneficiaryTrust)
              }
            )
            .success
            .value,
          answers.set(AreBeneficiariesKnownPage, false).success.value
        ) ++ (testName match {
          case "individual" =>
            Seq(
              answers.remove(BeneficiaryHasNinoPage(1)).success.value,
              answers.remove(BeneficiaryNoNinoReasonPage(1)).success.value,
              answers.set(BeneficiaryHasNinoPage(1), true).success.value
            )
          case _ => Seq(answers.remove(BeneficiaryTrustNamePage(1)).success.value)
        })
        invalidAnswers.foreach { data =>
          val service = mock[UserAnswersService]
          val app = applicationBuilder(Some(data), usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            Seq(GET, POST).foreach { method =>
              val result = route(
                app,
                FakeRequest(method, routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, NormalMode).url)
                  .withFormUrlEncodedBody("value" -> "12.34")
              ).value
              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
            }
            verifyNoInteractions(service)
          }
        }
      }

      "must reject invalid route indexes and recover for a beneficiary that does not exist" in {
        val app = applicationBuilder(Some(answers), usesSession = true).build()
        running(app) {
          testInvalidBeneficiaryIndexes.foreach { index =>
            status(
              route(
                app,
                FakeRequest(GET, routes.BeneficiaryIhtPayableController.onPageLoad(srn, index, NormalMode).url)
              ).value
            ) mustBe NOT_FOUND
          }
          val result =
            route(
              app,
              FakeRequest(GET, routes.BeneficiaryIhtPayableController.onPageLoad(srn, 2, NormalMode).url)
            ).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
