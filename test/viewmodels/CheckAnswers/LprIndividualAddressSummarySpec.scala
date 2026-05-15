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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import play.api.libs.json.Json
import models.{CheckMode, LprAddress}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class LprIndividualAddressSummarySpec extends SpecBase {

  "LprIndividualAddressSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      LprIndividualAddressSummary.row(srn, emptyUserAnswers) mustBe None
    }

    "must return a row when data is present" in {

      val address = LprAddress(
        addressLine1 = "33 Fake Street",
        addressLine2 = Some("Fake Area"),
        addressLine3 = Some("Fake County"),
        addressLine4 = Some("Fakeville"),
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "lprDetails" -> Json.obj(
            "individual" -> Json.toJson(address)
          )
        )
      )

      val result = LprIndividualAddressSummary.row(
        srn,
        userAnswers,
        countryNameForCode = code => if (code == "GB") "United Kingdom" else code
      )

      result mustBe defined
      result.value.key.content mustBe Text(messages("lprIndividualAddress.checkYourAnswersLabel"))
      result.value.value.content mustBe HtmlContent(
        "33 Fake Street<br>Fake Area<br>Fake County<br>Fakeville<br>ZZ1 1ZZ<br>United Kingdom"
      )
      result.value.actions.value.items.head.href mustBe
        controllers.routes.AddressLookupStartController.start(srn, CheckMode).url
    }

    "must fall back to the country code when a country name cannot be found" in {

      val address = LprAddress(
        addressLine1 = "33 Fake Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = None,
        country = "XX"
      )

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "lprDetails" -> Json.obj(
            "individual" -> Json.toJson(address)
          )
        )
      )

      val result = LprIndividualAddressSummary.row(srn, userAnswers, countryNameForCode = identity)

      result.value.value.content mustBe HtmlContent("33 Fake Street<br>XX")
    }
  }
}
