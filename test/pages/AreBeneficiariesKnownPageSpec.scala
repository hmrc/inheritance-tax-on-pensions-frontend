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

package pages

import base.SpecBase
import play.api.libs.json.JsPath
import models.beneficiary.BeneficiaryType
import pages.beneficiary.BeneficiaryTypePage

class AreBeneficiariesKnownPageSpec extends SpecBase {

  "AreBeneficiariesKnownPage" - {

    "must use the expected path" in {
      AreBeneficiariesKnownPage.path mustEqual JsPath \ "areBeneficiariesKnown"
    }

    "must use the expected key" in {
      AreBeneficiariesKnownPage.toString mustEqual "areBeneficiariesKnown"
    }

    "must remove all beneficiaries when Yes changes to No and preserve unrelated answers" in {
      val userAnswers = emptyUserAnswers
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value
        .set(IhtPayablePage, BigDecimal(100))
        .success
        .value
        .set(DidPrSubmitPage, true)
        .success
        .value
        .set(BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .success
        .value
        .set(BeneficiaryTypePage(1), BeneficiaryType.Trust)
        .success
        .value

      val result = userAnswers.set(AreBeneficiariesKnownPage, false).success.value

      result mustEqual userAnswers.copy(
        data = (userAnswers.data - "beneficiaries") ++
          play.api.libs.json.Json.obj("areBeneficiariesKnown" -> false)
      )
      (result.set(AreBeneficiariesKnownPage, true).success.value.data \ "beneficiaries").isDefined mustBe false
    }

    "must allow No to be saved when there are no beneficiaries" in {
      val result = emptyUserAnswers.set(AreBeneficiariesKnownPage, false).success.value

      result.get(AreBeneficiariesKnownPage) mustBe Some(false)
      (result.data \ "beneficiaries").isDefined mustBe false
    }
  }
}
