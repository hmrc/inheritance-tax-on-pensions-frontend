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

package viewmodels.CheckAnswers

import play.api.test.Helpers.stubMessages
import pages.AreBeneficiariesKnownPage
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class AreBeneficiariesKnownSummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "AreBeneficiariesKnownSummary" - {

    "must return None when data is not present" in {
      val result = AreBeneficiariesKnownSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when the beneficiaries are known" in {
      val userAnswers = emptyUserAnswers
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value

      val result = AreBeneficiariesKnownSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("areBeneficiariesKnown.checkYourAnswersLabel"))
      result.value.value mustEqual Value(Text("site.yes"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.routes.AreBeneficiariesKnownController.onPageLoad(srn, CheckMode).url,
          Text("site.change"),
          visuallyHiddenText = Some("areBeneficiariesKnown.change.hidden")
        )
      )
    }

    "must return a row when the beneficiaries are not known" in {
      val userAnswers = emptyUserAnswers
        .set(AreBeneficiariesKnownPage, false)
        .success
        .value

      val result = AreBeneficiariesKnownSummary.row(srn, userAnswers)

      result.value.value mustEqual Value(Text("site.no"))
    }
  }
}
