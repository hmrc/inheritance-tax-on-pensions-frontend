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

import viewmodels.CheckAnswers.beneficiary.BeneficiaryHasNinoSummary
import models.CheckMode
import pages.beneficiary.BeneficiaryHasNinoPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryHasNinoSummarySpec extends AnyFreeSpec with SpecBase {

  "BeneficiaryHasNinoSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {
      BeneficiaryHasNinoSummary.row(srn, testIndex, emptyUserAnswers) mustBe None
    }

    Seq(true -> "site.yes", false -> "site.no").foreach { case (answer, messageKey) =>
      s"must return a row when the answer is $answer" in {
        val userAnswers = emptyUserAnswers.set(BeneficiaryHasNinoPage(testIndex), answer).success.value

        val result = BeneficiaryHasNinoSummary.row(srn, testIndex, userAnswers)

        result mustBe defined
        result.value.key.content mustBe Text(messages("beneficiaryHasNino.checkYourAnswersLabel"))
        result.value.value.content mustBe Text(messages(messageKey))
        result.value.actions.value.items.head.href mustBe
          controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, testIndex, CheckMode).url
      }
    }
  }
}
