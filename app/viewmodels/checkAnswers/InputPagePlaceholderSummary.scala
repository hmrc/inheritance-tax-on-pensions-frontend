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

package viewmodels.checkAnswers

import viewmodels.implicits._
import play.twirl.api.HtmlFormat
import pages.InputPagePlaceholderPage
import controllers.routes
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.{CheckMode, UserAnswers}
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object InputPagePlaceholderSummary {

  def row(answers: UserAnswers, srn: Srn)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(InputPagePlaceholderPage).map { answer =>
      SummaryListRowViewModel(
        key = "inputPagePlaceholder.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.InputPagePlaceholderController.onPageLoad(srn, CheckMode).url)
            .withVisuallyHiddenText(messages("inputPagePlaceholder.change.hidden"))
        )
      )
    }
}
