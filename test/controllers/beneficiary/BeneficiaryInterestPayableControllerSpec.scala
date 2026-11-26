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
import base.SpecBase
import models._
import pages.beneficiary._
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

class BeneficiaryInterestPayableControllerSpec extends SpecBase {

  private val answers = individualBeneficiaryUserAnswers

  "BeneficiaryInterestPayableController" - {
    "must populate the shared view for the selected beneficiary" in {
      val app = applicationBuilder(Some(answers), usesSession = true).build()
      running(app) {
        val result = route(
          app,
          FakeRequest(GET, routes.BeneficiaryInterestPayableController.onPageLoad(srn, testIndex, NormalMode).url)
        ).value
        status(result) mustBe OK
      }
    }

    "must save the amount and follow the next page" in {
      val savedAnswers = answers.set(BeneficiaryInterestPayablePage(testIndex), BigDecimal("12.34")).success.value
      val service = mock[UserAnswersService]
      when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(savedAnswers)))
      val app = applicationBuilder(Some(answers), usesSession = true)
        .overrides(bind[UserAnswersService].toInstance(service))
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(POST, routes.BeneficiaryInterestPayableController.onSubmit(srn, testIndex, NormalMode).url)
            .withFormUrlEncodedBody("value" -> "12.34")
        ).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.BeneficiaryListController.onPageLoad(srn).url
      }
    }
  }
}
