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
import pages.{DidPrSubmitPage, IndividualNamePage, PrTypePage}
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class DidPrSubmitSummarySpec extends SpecBase {

  private implicit val testMessages: Messages = stubMessages()

  "DidPrSubmitSummary" - {
    "must return None when the question has not been answered" in {
      DidPrSubmitSummary.row(srn, emptyUserAnswers) mustBe None
    }

    Seq(PrType.Individual -> JourneyRole.PrIndividual, PrType.Organisation -> JourneyRole.PrOrganisation).foreach {
      case (prType, role) =>
        s"must show the $prType PR's first name and surname without the title or middle name" in {
          val answers = emptyUserAnswers
            .set(PrTypePage, prType)
            .success
            .value
            .set(IndividualNamePage(role), IndividualName(Some("Dr"), "Firstname", Some("Middlename"), "Surname"))
            .success
            .value
            .set(DidPrSubmitPage, true)
            .success
            .value

          val row = DidPrSubmitSummary.row(srn, answers).value
          row.key.content mustBe Text(testMessages("didPrSubmit.checkYourAnswersLabel"))
          row.value.content mustBe Text("Firstname Surname")
          val change = row.actions.value.items.head
          change.href mustBe controllers.routes.DidPrSubmitController.onPageLoad(srn, CheckMode).url
          change.visuallyHiddenText mustBe Some(testMessages("didPrSubmit.change.hidden"))
        }

        s"must return None when the $prType PR submitted but their name is missing" in {
          val answers = emptyUserAnswers
            .set(PrTypePage, prType)
            .success
            .value
            .set(DidPrSubmitPage, true)
            .success
            .value

          DidPrSubmitSummary.row(srn, answers) mustBe None
        }
    }

    "must show Someone else even when no PR name is available" in {
      val answers = emptyUserAnswers.set(DidPrSubmitPage, false).success.value
      DidPrSubmitSummary.row(srn, answers).value.value.content mustBe Text(testMessages("didPrSubmit.someoneElse"))
    }

    "must treat the PR name as plain text" in {
      val answers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.PrIndividual),
          IndividualName(None, "<Firstname>", None, "O'Name & Surname")
        )
        .success
        .value
        .set(DidPrSubmitPage, true)
        .success
        .value

      DidPrSubmitSummary.row(srn, answers).value.value.content mustBe Text("<Firstname> O'Name & Surname")
    }
  }
}
