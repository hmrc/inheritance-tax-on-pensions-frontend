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
import play.api.libs.json.{JsError, JsString, Json}

class BeneficiaryTypeSpec extends SpecBase {

  "BeneficiaryType" - {

    "must read individual and trust from JSON" in {
      JsString("individual").validate[BeneficiaryType].asOpt.value mustBe BeneficiaryType.Individual
      JsString("trust").validate[BeneficiaryType].asOpt.value mustBe BeneficiaryType.Trust
    }

    "must reject other JSON values" in {
      JsString("organisation").validate[BeneficiaryType] mustBe JsError("error.invalid")
    }

    "must write individual and trust to JSON" in {
      Json.toJson[BeneficiaryType](BeneficiaryType.Individual) mustBe JsString("individual")
      Json.toJson[BeneficiaryType](BeneficiaryType.Trust) mustBe JsString("trust")
    }
  }
}
