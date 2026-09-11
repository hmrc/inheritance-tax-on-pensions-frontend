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

import play.api.libs.json.Json
import base.SpecBase
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}

class NumberOfBeneficiariesSummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "NumberOfBeneficiariesSummary" - {

    "must return None when data is not present" in {
      val result = NumberOfBeneficiariesSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when the PR has submitted the notice and beneficiaries are known with no beneficiaries" in {
      val userAnswers = emptyUserAnswers
        .set(DidPrSubmitPage, true)
        .success
        .value
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value

      val result = NumberOfBeneficiariesSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("numberOfBeneficiaries.checkYourAnswersLabel"))
      result.value.value mustEqual Value(Text("0"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.beneficiary.routes.BeneficiaryListController.onPageLoad(srn).url,
          Text("site.change"),
          visuallyHiddenText = Some("numberOfBeneficiaries.change.hidden")
        )
      )
    }

    "must return a row when the PR has submitted the notice and beneficiaries are known with beneficiaries present" in {
      val userAnswers = emptyUserAnswers
        .copy(
          data = Json.obj(
            "beneficiaries" -> Json.arr(
              Json.obj(
                "beneficiaryType" -> "individual",
                "hasNino" -> true
              ),
              Json.obj(
                "beneficiaryType" -> "individual"
              )
            )
          )
        )
        .set(DidPrSubmitPage, true)
        .success
        .value
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value

      val result = NumberOfBeneficiariesSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("numberOfBeneficiaries.checkYourAnswersLabel"))
      result.value.value mustEqual Value(Text("2"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.beneficiary.routes.BeneficiaryListController.onPageLoad(srn).url,
          Text("site.change"),
          visuallyHiddenText = Some("numberOfBeneficiaries.change.hidden")
        )
      )
    }
  }
}
