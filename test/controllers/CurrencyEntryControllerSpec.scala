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
import services.UserAnswersService
import pages.{AreBeneficiariesKnownPage, IhtPayablePage, QuestionPage}
import play.api.inject.bind
import views.html.CurrencyEntryView
import base.SpecBase
import play.api.libs.json.{JsPath, Json}
import models.{CheckMode, NormalMode, UserAnswers}
import viewmodels.CurrencyEntryPageViewModel
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import utils.TestCurrencyEntry
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import forms.CurrencyEntryFormProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class CurrencyEntryControllerSpec extends SpecBase {
  private val answers = emptyUserAnswers.set(AreBeneficiariesKnownPage, false).success.value
  private val entry = TestCurrencyEntry(maximumError = Some("ihtPayable.error.invalid"))
  private val secondAmountPage = new QuestionPage[BigDecimal] {
    override def path: JsPath = (JsPath \ "beneficiaries")(1) \ "amount"
  }

  "CurrencyEntryController" - {
    Seq(NormalMode, CheckMode).foreach { mode =>
      s"must populate the shared view with the saved amount in $mode" in {
        val savedAnswers = answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
        val app = applicationBuilder(Some(savedAnswers), usesSession = true).build()
        running(app) {
          val controller = app.injector.instanceOf[CurrencyEntryController]
          val request = FakeRequest(GET, "/currency-entry")
          val result = call(controller.onPageLoad(srn, mode, entry), request)
          val view = app.injector.instanceOf[CurrencyEntryView]
          val form = new CurrencyEntryFormProvider()(entry).fill(BigDecimal("1234.56"))
          val page = CurrencyEntryPageViewModel(entry, savedAnswers, srn, mode)(using messages(app))
          status(result) mustBe OK
          contentAsString(result) mustBe view(form, page)(using request, messages(app)).toString
        }
      }

      s"must save only the configured answer and follow its next page in $mode" in {
        val configuredEntry = entry.copy(page = secondAmountPage)
        val originalAnswers = answers
          .copy(data = answers.data ++ Json.obj("beneficiaries" -> Json.arr(Json.obj("amount" -> 75.25), Json.obj())))
          .set(IhtPayablePage, BigDecimal(50))
          .success
          .value
        val savedAnswers = originalAnswers.set(secondAmountPage, BigDecimal("1234.56")).success.value
        val service = mock[UserAnswersService]
        when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(savedAnswers)))
        val app = applicationBuilder(Some(originalAnswers), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(service))
          .build()
        running(app) {
          val controller = app.injector.instanceOf[CurrencyEntryController]
          val result = call(
            controller.onSubmit(srn, mode, configuredEntry),
            FakeRequest(POST, "/currency-entry").withFormUrlEncodedBody("value" -> "1,234.56")
          )
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe configuredEntry.nextPage(srn, mode, savedAnswers).url
          val capturedAnswers = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).set(capturedAnswers.capture())(using any(), any())
          capturedAnswers.getValue mustBe savedAnswers
        }
      }
    }

    Seq("", "10%").foreach { input =>
      s"must reject invalid input '$input' without saving" in {
        val service = mock[UserAnswersService]
        val app = applicationBuilder(Some(answers), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(service))
          .build()
        running(app) {
          val controller = app.injector.instanceOf[CurrencyEntryController]
          val result = call(
            controller.onSubmit(srn, NormalMode, entry),
            FakeRequest(POST, "/currency-entry").withFormUrlEncodedBody("value" -> input)
          )
          status(result) mustBe BAD_REQUEST
          verifyNoInteractions(service)
        }
      }
    }

    Seq(None, Some(emptyUserAnswers)).zipWithIndex.foreach { case (data, index) =>
      Seq(GET, POST).foreach { method =>
        s"must recover without saving for invalid journey state $index on $method" in {
          val service = mock[UserAnswersService]
          val app = applicationBuilder(data, usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val controller = app.injector.instanceOf[CurrencyEntryController]
            val action = if (method == GET) {
              controller.onPageLoad(srn, NormalMode, entry)
            } else {
              controller.onSubmit(srn, NormalMode, entry)
            }
            val result = call(action, FakeRequest(method, "/currency-entry").withFormUrlEncodedBody("value" -> "12.34"))
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
            verifyNoInteractions(service)
          }
        }
      }
    }

    "must recover rather than continue when saving fails" in {
      val service = mock[UserAnswersService]
      when(service.set(any())(using any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", 500))))
      val app = applicationBuilder(Some(answers), usesSession = true)
        .overrides(bind[UserAnswersService].toInstance(service))
        .build()
      running(app) {
        val controller = app.injector.instanceOf[CurrencyEntryController]
        val result = call(
          controller.onSubmit(srn, NormalMode, entry),
          FakeRequest(POST, "/currency-entry").withFormUrlEncodedBody("value" -> "12.34")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
