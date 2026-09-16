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

import viewmodels.implicits._
import pages.{AreBeneficiariesKnownPage, IhtPayablePage}
import controllers.routes
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.{CheckMode, UserAnswers}
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

import java.util.Locale
import java.text.NumberFormat

object IhtPayableSummary {
  def row(srn: Srn, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      known <- answers.get(AreBeneficiariesKnownPage) if !known
      amount <- answers.get(IhtPayablePage)
    } yield SummaryListRowViewModel(
      key = "ihtPayable.checkYourAnswersLabel",
      value = ValueViewModel(NumberFormat.getCurrencyInstance(Locale.UK).format(amount.bigDecimal)),
      actions = Seq(
        ActionItemViewModel("site.change", routes.IhtPayableController.onPageLoad(srn, CheckMode).url)
          .withVisuallyHiddenText(messages("ihtPayable.change.hidden"))
      )
    )
}
