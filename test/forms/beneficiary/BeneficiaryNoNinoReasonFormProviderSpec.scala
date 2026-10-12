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

import forms.mappings.Regex
import forms.behaviours.StringFieldBehaviours
import forms.beneficiary.BeneficiaryNoNinoReasonFormProvider
import play.api.data.FormError

class BeneficiaryNoNinoReasonFormProviderSpec extends StringFieldBehaviours with Regex {

  val requiredKey = "beneficiaryNoNinoReason.error.required"
  val invalidCharactersKey = "beneficiaryNoNinoReason.error.invalid"

  val form = new BeneficiaryNoNinoReasonFormProvider()()

  ".beneficiaryNoNinoReason" - {

    val fieldName = "beneficiaryNoNinoReason"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        "Isn't an adult"
      )
    )

    behave.like(
      mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    )

    Seq(
      "%" -> "percent sign",
      "$" -> "dollar sign",
      "£" -> "pound sign",
      "abc\rabc" -> "carriage return",
      "abc\nabc" -> "newline",
      "\"" -> "quote",
      "&" -> "ampersand"
    ).foreach((invalidCharacter, description) =>
      s"for $description" - {
        behave.like(
          fieldContainsRegexError(
            form,
            fieldName,
            invalidCharacter,
            error = FormError(fieldName, invalidCharactersKey, Seq(reasonNoNinoRegex))
          )
        )
      }
    )
  }
}
