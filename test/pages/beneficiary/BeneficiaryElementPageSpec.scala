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

import base.beneficiary.BeneficiarySpecBase
import models.beneficiary._

class BeneficiaryElementPageSpec extends BeneficiarySpecBase {

  "BeneficiaryElementPage" - {

    "can be used to remove a beneficiary" in {

      val allBeneficiaries = userAnswersWithBeneficiaries.get(BeneficiariesPage())
      allBeneficiaries.get.beneficiaries.size mustEqual 2

      val uaAfterRemoval =
        userAnswersWithBeneficiaries.remove(BeneficiaryElementPage(testIndex)).get // example for removal
      val allBeneficiaryTypesAfterRemoval = uaAfterRemoval.get(BeneficiariesPage())
      allBeneficiaryTypesAfterRemoval.get.beneficiaries.size mustEqual 1
    }
  }
}
