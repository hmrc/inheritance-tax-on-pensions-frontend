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
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.{CheckMode, UserAnswers}
import pages.beneficiary.BeneficiaryTypePage
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object BeneficiaryTypeSummary {

  def row(srn: Srn, index: Int, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BeneficiaryTypePage(index)).map { answer =>
      SummaryListRowViewModel(
        key = "beneficiaryType.checkYourAnswersLabel",
        value = ValueViewModel(messages(s"beneficiaryType.${answer.toString}")),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, index, CheckMode).url
          )
            .withVisuallyHiddenText(messages("beneficiaryType.change.hidden"))
        )
      )
    }
}
