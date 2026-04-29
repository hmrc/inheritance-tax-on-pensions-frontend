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
import pages.BirthDeathDatesPage
import controllers.routes
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.{CheckMode, UserAnswers}
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

import java.util.Locale
import java.time.format.DateTimeFormatter

object BirthDeathDatesSummary {

  private val cyaDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

  def row(srn: Srn, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BirthDeathDatesPage).map { answer =>

      val value = s"${answer.dateOfBirth.format(cyaDateFormatter)} to ${answer.dateOfDeath.format(cyaDateFormatter)}"

      SummaryListRowViewModel(
        key = "birthDeathDates.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.BirthDeathDatesController.onPageLoad(srn, CheckMode).url)
            .withVisuallyHiddenText(messages("birthDeathDates.change.hidden"))
        )
      )
    }
}
