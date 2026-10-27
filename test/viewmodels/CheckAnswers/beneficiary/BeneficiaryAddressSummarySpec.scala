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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import models.PrAddress
import pages.beneficiary.BeneficiaryAddressPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryAddressSummarySpec extends SpecBase {
  private val answers = individualBeneficiaryUserAnswers

  "BeneficiaryAddressSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {
      BeneficiaryAddressSummary.row(
        srn,
        0,
        answers.remove(BeneficiaryAddressPage(0)).success.value
      ) mustBe None
    }

    "must render the address without its country and an indexed Change link" in {
      val address = PrAddress(
        addressLine1 = "21 RB Road",
        addressLine2 = Some("RBtown"),
        addressLine3 = None,
        addressLine4 = None,
        postCode = Some("LL1 4LL"),
        country = "GB"
      )
      val data = answers.set(BeneficiaryAddressPage(0), address).success.value

      val row = BeneficiaryAddressSummary.row(srn, 0, data).value
      row.key.content mustBe Text(messages("beneficiaryAddress.checkYourAnswersLabel"))
      row.value.content mustBe HtmlContent("21 RB Road<br>RBtown<br>LL1 4LL")
      row.actions.value.items.head.href mustBe controllers.beneficiary.routes.BeneficiaryAddressController
        .start(srn, 0, models.CheckMode)
        .url
    }

    "must skip absent optional lines without appending the country" in {
      val address = PrAddress(
        addressLine1 = "33 AB Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postCode = None,
        country = "FR"
      )
      val data = answers.set(BeneficiaryAddressPage(0), address).success.value

      val row = BeneficiaryAddressSummary.row(srn, 0, data).value
      row.value.content mustBe HtmlContent("33 AB Street")
    }
  }
}
