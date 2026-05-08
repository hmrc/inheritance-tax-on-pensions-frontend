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
import pages.OrganisationNamePage
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class LprOrganisationNameSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "LprOrganisationNameSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = LprOrganisationNameSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when data is present" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationNamePage, "Test Org")
        .success
        .value

      val result = LprOrganisationNameSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("organisationName.checkYourAnswersLabel"))
      result.get.value.content mustBe Text("Test Org")
      result.get.actions.get.items.head.href mustBe
        controllers.routes.OrganisationNameController.onPageLoad(srn, CheckMode).url
    }
  }
}
