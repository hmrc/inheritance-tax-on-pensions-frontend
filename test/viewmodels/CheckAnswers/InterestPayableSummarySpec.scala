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
import pages.{AreBeneficiariesKnownPage, InterestPayablePage}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class InterestPayableSummarySpec extends SpecBase {
  implicit val messages: Messages = stubMessages()

  "InterestPayableSummary" - {
    Seq(
      "0" -> "\u00a30.00",
      "1234" -> "\u00a31,234.00",
      "1234.5" -> "\u00a31,234.50",
      "1234.56" -> "\u00a31,234.56",
      "999999999.99" -> "\u00a3999,999,999.99"
    ).foreach { case (amount, formatted) =>
      s"must display $amount in pounds and pence with a Change link" in {
        val answers = emptyUserAnswers
          .set(AreBeneficiariesKnownPage, false)
          .success
          .value
          .set(InterestPayablePage, BigDecimal(amount))
          .success
          .value
        val row = InterestPayableSummary.row(srn, answers).value
        row.key mustBe Key(Text("interestPayable.checkYourAnswersLabel"))
        row.value mustBe Value(Text(formatted))
        row.actions.value.items mustBe Seq(
          ActionItem(
            controllers.routes.InterestPayableController.onPageLoad(srn, CheckMode).url,
            Text("site.change"),
            visuallyHiddenText = Some("interestPayable.change.hidden")
          )
        )
      }
    }

    "must omit the row when the amount is missing" in {
      val answers = emptyUserAnswers.set(AreBeneficiariesKnownPage, false).success.value
      InterestPayableSummary.row(srn, answers) mustBe None
    }

    "must omit stale amounts when beneficiaries are known" in {
      val answers = emptyUserAnswers
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value
        .set(InterestPayablePage, BigDecimal("10"))
        .success
        .value
      InterestPayableSummary.row(srn, answers) mustBe None
    }

    "must omit amounts when beneficiaries known has not been answered" in {
      val answers = emptyUserAnswers.set(InterestPayablePage, BigDecimal("10")).success.value
      InterestPayableSummary.row(srn, answers) mustBe None
    }
  }
}
