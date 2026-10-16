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

import play.api.test.Helpers.running
import pages.AreBeneficiariesKnownPage
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value
import models.beneficiary.BeneficiaryType
import models.CheckMode
import pages.beneficiary.{BeneficiaryIhtPayablePage, BeneficiaryTypePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryIhtPayableSummarySpec extends SpecBase {
  private val answers = individualBeneficiaryUserAnswers

  "BeneficiaryIhtPayableSummary" - {
    "must format amounts in pounds and pence with an indexed Change link" in {
      val app = applicationBuilder().build()
      running(app) {
        given Messages = messages(app)
        Seq("0" -> "\u00a30.00", "1234.5" -> "\u00a31,234.50", "999999999.99" -> "\u00a3999,999,999.99")
          .foreach { case (amount, expected) =>
            val data = answers.set(BeneficiaryIhtPayablePage(0), BigDecimal(amount)).success.value
            val row = BeneficiaryIhtPayableSummary.row(srn, 0, data).value
            row.value mustBe Value(Text(expected))
            row.actions.value.items.head.href mustBe controllers.beneficiary.routes.BeneficiaryIhtPayableController
              .onPageLoad(srn, 0, CheckMode)
              .url
          }
      }
    }

    "must omit absent and inapplicable amounts" in {
      val app = applicationBuilder().build()
      running(app) {
        given Messages = messages(app)
        val data = answers.set(BeneficiaryIhtPayablePage(0), BigDecimal("12.34")).success.value
        BeneficiaryIhtPayableSummary.row(srn, 0, answers) mustBe None
        BeneficiaryIhtPayableSummary.row(srn, 1, data) mustBe None
        BeneficiaryIhtPayableSummary.row(srn, 0, data.set(AreBeneficiariesKnownPage, false).success.value) mustBe None
        BeneficiaryIhtPayableSummary.row(
          srn,
          0,
          data.set(BeneficiaryTypePage(0), BeneficiaryType.Trust).success.value
        ) mustBe None
      }
    }
  }
}
