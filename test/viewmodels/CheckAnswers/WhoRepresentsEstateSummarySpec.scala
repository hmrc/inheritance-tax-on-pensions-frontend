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

import pages.WhoRepresentsEstatePage
import models.{CheckMode, RepresentativeType}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class WhoRepresentsEstateSummarySpec extends AnyFreeSpec with SpecBase {

  "WhoRepresentsEstateSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = WhoRepresentsEstateSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }
    "must return a row when data is selected" in {

      val userAnswers = emptyUserAnswers
        .set(WhoRepresentsEstatePage, RepresentativeType.PR)
        .success
        .value

      val result = WhoRepresentsEstateSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("whoRepresentsEstate.checkYourAnswersLabel"))
      result.get.value.content mustBe Text(messages("whoRepresentsEstate.checkYourAnswers.02"))
      result.get.actions.get.items.head.href mustBe
        controllers.routes.WhoRepresentsEstateController.onPageLoad(srn, CheckMode).url
    }

    "must return a row when PPR is selected" in {

      val userAnswers = emptyUserAnswers
        .set(WhoRepresentsEstatePage, RepresentativeType.PPR)
        .success
        .value

      val result = WhoRepresentsEstateSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("whoRepresentsEstate.checkYourAnswersLabel"))
      result.get.value.content mustBe Text(messages("whoRepresentsEstate.checkYourAnswers.01"))
      result.get.actions.get.items.head.href mustBe
        controllers.routes.WhoRepresentsEstateController.onPageLoad(srn, CheckMode).url
    }
  }
}
