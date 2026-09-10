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
import pages.AreBeneficiariesKnownPage
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.beneficiary.Beneficiaries
import models.UserAnswers
import pages.beneficiary.BeneficiariesPage
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object NumberOfBeneficiariesSummary {

  def row(srn: Srn, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AreBeneficiariesKnownPage).collect { case true =>
      val value = answers
        .get[Beneficiaries](BeneficiariesPage())
        .map(_.beneficiaries.length.toString())
        .getOrElse("0")

      SummaryListRowViewModel(
        key = "numberOfBeneficiaries.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.beneficiary.routes.BeneficiaryListController.onPageLoad(srn).url
          )
            .withVisuallyHiddenText(messages("numberOfBeneficiaries.change.hidden"))
        )
      )
    }
}
