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

package models

import base.SpecBase

class PensionSchemeIdSpec extends SpecBase {

  "PensionSchemeId" - {

    "fold" - {
      "must call f1 for PsaId" in {
        val psaId = PensionSchemeId.PsaId("PSA123")
        val result = psaId.fold(
          psa => s"PSA: ${psa.value}",
          psp => s"PSP: ${psp.value}"
        )
        result mustBe "PSA: PSA123"
      }

      "must call f2 for PspId" in {
        val pspId = PensionSchemeId.PspId("PSP456")
        val result = pspId.fold(
          psa => s"PSA: ${psa.value}",
          psp => s"PSP: ${psp.value}"
        )
        result mustBe "PSP: PSP456"
      }
    }

    "isPSP" - {
      "must return true for PspId" in {
        val pspId = PensionSchemeId.PspId("PSP456")
        pspId.isPSP mustBe true
      }

      "must return false for PsaId" in {
        val psaId = PensionSchemeId.PsaId("PSA123")
        psaId.isPSP mustBe false
      }
    }
  }
}
