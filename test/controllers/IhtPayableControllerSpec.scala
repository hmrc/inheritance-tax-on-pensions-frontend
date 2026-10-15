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
import pages.{AreBeneficiariesKnownPage, IhtPayablePage}
import play.api.inject.bind
import base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import models.{CheckMode, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, verifyNoInteractions, when}

import scala.concurrent.Future

class IhtPayableControllerSpec extends SpecBase {
  private val answers = emptyUserAnswers.set(AreBeneficiariesKnownPage, false).success.value

  "IhtPayableController" - {
    Seq(NormalMode, CheckMode).foreach { mode =>
      Seq(true, false).foreach { isPsa =>
        s"must return OK in $mode for isPsa=$isPsa" in {
          val app = applicationBuilder(Some(answers), isPsa = isPsa, usesSession = true).build()
          running(app) {
            val result = route(app, FakeRequest(GET, routes.IhtPayableController.onPageLoad(srn, mode).url)).value
            status(result) mustBe OK
          }
        }

        s"must save the amount and return to CYA in $mode for isPsa=$isPsa" in {
          val service = mock[UserAnswersService]
          when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(answers)))
          val app = applicationBuilder(Some(answers), isPsa = isPsa, usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val result = route(
              app,
              FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, mode).url)
                .withFormUrlEncodedBody("value" -> "1,234.56")
            ).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.CheckYourAnswersController
              .onPageLoad(srn)
              .url
            val saved = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(service).set(saved.capture())(using any(), any())
            saved.getValue mustBe answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
          }
        }
      }
    }

    Seq("", "10%")
      .foreach { input =>
        s"must reject invalid input '$input' without saving" in {
          val service = mock[UserAnswersService]
          val app = applicationBuilder(Some(answers), usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val result = route(
              app,
              FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, NormalMode).url)
                .withFormUrlEncodedBody("value" -> input)
            ).value
            status(result) mustBe BAD_REQUEST
            verifyNoInteractions(service)
          }
        }
      }

    Seq(
      None,
      Some(emptyUserAnswers),
      Some(emptyUserAnswers.set(AreBeneficiariesKnownPage, true).success.value)
    ).zipWithIndex.foreach { case (data, index) =>
      Seq(GET, POST).foreach { method =>
        s"must recover for invalid journey state $index on $method" in {
          val service = mock[UserAnswersService]
          val app =
            applicationBuilder(data, usesSession = true).overrides(bind[UserAnswersService].toInstance(service)).build()
          running(app) {
            val result = route(
              app,
              FakeRequest(method, routes.IhtPayableController.onPageLoad(srn, NormalMode).url)
                .withFormUrlEncodedBody("value" -> "12.34")
            ).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
            verifyNoInteractions(service)
          }
        }
      }
    }

    "must not continue to CYA when saving fails" in {
      val service = mock[UserAnswersService]
      when(service.set(any())(using any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", 500))))
      val app = applicationBuilder(Some(answers), usesSession = true)
        .overrides(bind[UserAnswersService].toInstance(service))
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, NormalMode).url)
            .withFormUrlEncodedBody("value" -> "12.34")
        ).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
