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
import pages.IndividualNamePage
import models.{CheckMode, IndividualName, JourneyRole}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class LprOrganisationPrNameSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "LprOrganisationPrNameSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = LprOrganisationPrNameSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when data is present" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.LprOrganisation),
          IndividualName(
            title = Some("Ms"),
            firstForename = "Jane",
            secondForename = Some("Ann"),
            surname = "Doe"
          )
        )
        .success
        .value

      val result = LprOrganisationPrNameSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("lprOrganisationName.checkYourAnswersLabel"))
      result.get.value.content mustBe Text("Ms Jane Ann Doe")
      result.get.actions.get.items.head.href mustBe
        controllers.routes.IndividualNameController.onPageLoad(srn, CheckMode, JourneyRole.LprOrganisation).url
    }
  }
}
