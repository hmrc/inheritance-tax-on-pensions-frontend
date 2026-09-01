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

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class BeneficiaryTrustDetailsSpec extends SpecBase {

  "BeneficiaryTrustDetails" - {

    "must successfully read from json" in {
      val json = Json.obj("beneficiaryTrstName" -> trustName)

      json.validate[BeneficiaryTrustDetails] mustBe JsSuccess(BeneficiaryTrustDetails(trustName))
    }

    "must successfully write to json" in {
      val json = Json.toJson(BeneficiaryTrustDetails(trustName))

      (json \ "beneficiaryTrstName").as[String] mustBe trustName
    }

    "must fail when beneficiaryTrstName is missing" in {
      Json.obj().validate[BeneficiaryTrustDetails] mustBe a[JsError]
    }

    "must fail when beneficiaryTrstName has the wrong type" in {
      Json.obj("beneficiaryTrstName" -> 123).validate[BeneficiaryTrustDetails] mustBe a[JsError]
    }
  }
}
