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

import viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.UserAnswers
import pages.beneficiary.BeneficiaryInterestPayablePage
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

import java.util.Locale
import java.text.NumberFormat

object BeneficiaryInterestPayableSummary {
  def row(index: Int, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BeneficiaryInterestPayablePage(index)).map { amount =>
      SummaryListRowViewModel(
        key = "beneficiaryInterestPayable.checkYourAnswersLabel",
        value = ValueViewModel(NumberFormat.getCurrencyInstance(Locale.UK).format(amount.bigDecimal)),
        actions = Seq()
      )
    }
}
