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
import pages.HasNinoPage
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class HasNinoSummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "HasNinoSummary" - {

    "must return None when data is not present" in {
      val result = HasNinoSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when the deceased has a NINO" in {
      val userAnswers = emptyUserAnswers
        .set(HasNinoPage, true)
        .success
        .value

      val result = HasNinoSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("hasNino.checkYourAnswersLabel"))
      result.value.value mustEqual Value(Text("site.yes"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.routes.HasNinoController.onPageLoad(srn, CheckMode).url,
          Text("site.change"),
          visuallyHiddenText = Some("hasNino.change.hidden")
        )
      )
    }

    "must return a row when the deceased does not have a NINO" in {
      val userAnswers = emptyUserAnswers
        .set(HasNinoPage, false)
        .success
        .value

      val result = HasNinoSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("hasNino.checkYourAnswersLabel"))
      result.value.value mustEqual Value(Text("site.no"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.routes.HasNinoController.onPageLoad(srn, CheckMode).url,
          Text("site.change"),
          visuallyHiddenText = Some("hasNino.change.hidden")
        )
      )
    }
  }
}
