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

import pages.BirthDeathDatesPage
import models.{BirthDeathDates, CheckMode}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

import java.time.LocalDate

class BirthDeathDatesSummarySpec extends AnyFreeSpec with SpecBase {

  "BirthDeathDatesSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = BirthDeathDatesSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row with the birth and death dates on one line" in {

      val userAnswers = emptyUserAnswers
        .set(
          BirthDeathDatesPage,
          BirthDeathDates(
            dateOfBirth = LocalDate.of(1956, 1, 16),
            dateOfDeath = LocalDate.of(2025, 9, 20)
          )
        )
        .success
        .value

      val result = BirthDeathDatesSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("birthDeathDates.checkYourAnswersLabel"))
      result.get.value.content mustBe Text("16 Jan 1956 to 20 Sep 2025")
      result.get.actions.get.items.head.href mustBe
        controllers.routes.BirthDeathDatesController.onPageLoad(srn, CheckMode).url
    }
  }
}
