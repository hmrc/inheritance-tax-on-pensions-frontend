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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value
import pages.beneficiary.BeneficiaryInterestPayablePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryInterestPayableSummarySpec extends SpecBase {
  private val answers = individualBeneficiaryUserAnswers

  "BeneficiaryInterestPayableSummary" - {
    implicit val messages: Messages = stubMessages()

    "must format amounts in pounds and pence with no Change link" in {
      Seq("0" -> "£0.00", "1234.5" -> "£1,234.50", "999999999.99" -> "£999,999,999.99")
        .foreach { case (amount, expected) =>
          val data = answers.set(BeneficiaryInterestPayablePage(0), BigDecimal(amount)).success.value
          val row = BeneficiaryInterestPayableSummary.row(0, data).value
          row.value mustBe Value(Text(expected))
          row.actions.map(_.items).getOrElse(Nil) mustBe empty
        }
    }

    "must return None when data is not present" in {
      BeneficiaryInterestPayableSummary.row(0, answers) mustBe None
      val data = answers.set(BeneficiaryInterestPayablePage(0), BigDecimal("12.34")).success.value
      BeneficiaryInterestPayableSummary.row(1, data) mustBe None
    }
  }
}
