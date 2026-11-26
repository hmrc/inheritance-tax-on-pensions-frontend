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

package models.beneficiary

import base.beneficiary.BeneficiarySpecBase
import play.api.libs.json.{JsError, JsString, Json}
import helpers.FormatHelper
import pages.beneficiary.BeneficiariesPage

class BeneficiariesSpec extends BeneficiarySpecBase with FormatHelper {

  private val allBeneficiaries = userAnswersWithBeneficiaries.get(BeneficiariesPage())

  testFormat(allBeneficiaries.get)

  "must fail to read invalid beneficiary elements" in {
    Seq(
      JsString("text"),
      Json.obj("beneficiaryType" -> "organisation"),
      Json.obj("beneficiaryType" -> "individual", "beneficiaryDetails" -> "text"),
      Json.obj("beneficiaryType" -> "organisation", "beneficiaryDetails" -> "text")
    ).foreach { json =>
      json.validate[BeneficiaryElement] mustBe a[JsError]
    }
  }

  "must fail to read invalid beneficiary details" in {
    Seq(
      Json.obj("beneficiaryType" -> "individual", "beneficiaryDetails" -> Json.obj("individual" -> "text")),
      Json.obj("beneficiaryType" -> "individual", "beneficiaryDetails" -> Json.obj("trust" -> "text")),
      Json.obj(
        "beneficiaryType" -> "individual",
        "beneficiaryDetails" -> Json.obj("individual" -> "text", "trust" -> "text")
      )
    ).foreach { json =>
      json.validate[BeneficiaryElement] mustBe a[JsError]
    }
  }

  "must fail to read invalid beneficiaries" in {
    Seq(
      JsString("text"),
      Json.obj(),
      Json.obj("beneficiaries" -> "text")
    ).foreach { json =>
      json.validate[Beneficiaries] mustBe a[JsError]
    }
  }
}
