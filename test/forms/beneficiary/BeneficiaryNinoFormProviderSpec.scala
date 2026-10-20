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

package forms.beneficiary

import forms.behaviours.StringFieldBehaviours
import base.TestValues
import forms.beneficiary.BeneficiaryNinoFormProvider
import uk.gov.hmrc.domain.Nino
import play.api.data.FormError

import java.util.Locale

class BeneficiaryNinoFormProviderSpec extends StringFieldBehaviours with TestValues {

  val requiredKey = "beneficiaryNino.error.required"
  val invalidCharactersKey = "beneficiaryNino.error.invalid"

  val form = new BeneficiaryNinoFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        testNino
      )
    )

    "must bind valid national insurance number and ignore whitespace and lowercase" in {
      val formattedNino = s"  ${testNino.grouped(2).mkString("  ")}  ".toLowerCase(Locale.ROOT)
      val result = form.bind(Map("value" -> formattedNino))
      result.errors mustBe empty
      result.value mustBe Some(Nino(testNino))
    }

    behave.like(
      mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    )

    behave.like(
      fieldWithRegex(
        form,
        fieldName,
        "random",
        error = FormError(fieldName, invalidCharactersKey, Seq())
      )
    )
  }
}
