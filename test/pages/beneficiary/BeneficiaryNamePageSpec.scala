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

package pages.beneficiary

import base.SpecBase
import play.api.libs.json.JsPath
import models.beneficiary.BeneficiaryJourneyRole

class BeneficiaryNamePageSpec extends SpecBase {

  "BeneficiaryNamePage" - {

    "must use the beneficiary individual path for the beneficiary individual journey role" in {

      BeneficiaryNamePage(
        testIndex,
        BeneficiaryJourneyRole.BeneficiaryIndividual
      ).path mustEqual (JsPath \ "beneficiaries")(testIndex) \ "beneficiaryDetails" \ "individual"
    }

    "must use the beneficiary organisation path for the beneficiary organisation journey role" in {

      BeneficiaryNamePage(
        testIndex,
        BeneficiaryJourneyRole.BeneficiaryOrganisation
      ).path mustEqual (JsPath \ "beneficiaries")(testIndex) \ "beneficiaryDetails" \ "organisation"
    }

    "must use the unknown path for the unknown journey role" in {

      BeneficiaryNamePage(testIndex, BeneficiaryJourneyRole.Unknown).path mustEqual JsPath \ "unknown" \ "unknown"
    }

    "must use the journey role key as the page name" in {

      BeneficiaryNamePage(
        testIndex,
        BeneficiaryJourneyRole.Unknown
      ).toString mustEqual BeneficiaryJourneyRole.Unknown.key
    }
  }
}
