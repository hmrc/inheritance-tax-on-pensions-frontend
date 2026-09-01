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
import models.CheckMode
import pages.beneficiary.BeneficiaryTrustNamePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryTrustNameSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "BeneficiaryTrustNameSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {
      BeneficiaryTrustNameSummary.row(srn, testIndex, emptyUserAnswers) mustBe None
    }

    "must return the trust name" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryTrustNamePage(testIndex), trustName)
        .success
        .value

      val result = BeneficiaryTrustNameSummary.row(srn, testIndex, userAnswers).value

      result.key.content mustBe Text(messages("beneficiaryTrustName.checkYourAnswersLabel"))
      result.value.content mustBe Text(trustName)
      result.actions.value.items.head.href mustBe
        controllers.beneficiary.routes.BeneficiaryTrustNameController
          .onPageLoad(srn, testIndex, CheckMode)
          .url
    }
  }
}
