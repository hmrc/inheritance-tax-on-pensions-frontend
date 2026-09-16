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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import models.UserAnswers
import org.scalatest.TryValues
import play.api.libs.json.{JsPath, Json}

class IhtPayablePageSpec extends AnyFreeSpec with Matchers with TryValues {
  private val answers = UserAnswers("id", "S2400000018", "uuid", Json.obj("unrelated" -> "preserved"))

  "IhtPayablePage" - {
    "must use the latest EPID user-answers path" in {
      IhtPayablePage.path mustBe JsPath \ "ihtTaxInformation" \ "totalIhtPayable"
      IhtPayablePage.toString mustBe "totalIhtPayable"
    }

    "must round trip a numeric amount without changing other answers" in {
      val updated = answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
      updated.get(IhtPayablePage) mustBe Some(BigDecimal("1234.56"))
      (updated.data \ "ihtTaxInformation").get mustBe Json.obj("totalIhtPayable" -> BigDecimal("1234.56"))
      (updated.data \ "unrelated").as[String] mustBe "preserved"
    }

    "must clear the amount when beneficiaries become known or the answer is removed" in {
      val updated = answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
      updated.set(AreBeneficiariesKnownPage, true).success.value.get(IhtPayablePage) mustBe None
      updated.remove(AreBeneficiariesKnownPage).success.value.get(IhtPayablePage) mustBe None
    }

    "must preserve the amount when beneficiaries remain unknown" in {
      val updated = answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
      updated.set(AreBeneficiariesKnownPage, false).success.value.get(IhtPayablePage) mustBe Some(BigDecimal("1234.56"))
    }
  }
}
