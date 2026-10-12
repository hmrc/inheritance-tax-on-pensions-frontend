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

package viewmodels.CheckAnswers.beneficiary

import play.api.test.Helpers.stubMessages
import viewmodels.CheckAnswers.beneficiary.BeneficiaryNoNinoReasonSummary
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import controllers.beneficiary.routes
import models.CheckMode
import pages.beneficiary.BeneficiaryNoNinoReasonPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryNoNinoReasonSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "BeneficiaryNoNinoReasonSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = BeneficiaryNoNinoReasonSummary.row(srn, testIndex, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when data is present" in {

      val userAnswers = emptyUserAnswers
        .set(BeneficiaryNoNinoReasonPage(testIndex), "AA123456A")
        .success
        .value

      val result = BeneficiaryNoNinoReasonSummary.row(srn, testIndex, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("beneficiaryNoNinoReason.checkYourAnswersLabel"))
      result.get.value.content mustBe HtmlContent("AA123456A")
      result.get.actions.get.items.head.href mustBe
        routes.BeneficiaryNoNinoReasonController.onPageLoad(srn, testIndex, CheckMode).url
    }
  }
}
