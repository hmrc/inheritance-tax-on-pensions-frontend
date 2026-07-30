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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NoNinoReasonFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "noNinoReason.error.required"
  val invalidCharactersKey = "noNinoReason.error.invalid"
  val lengthKey = "noNinoReason.error.length"
  val maxLength = 160
  val validCharacterRegex = """^[a-zA-Z0-9\-’`'" \t,.@/&()]+$"""

  val form = new NoNinoReasonFormProvider()()

  ".noNinoReason" - {

    val fieldName = "noNinoReason"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        "reason for not having a NINO"
      )
    )

    behave.like(
      mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    )

    behave.like(
      fieldContainsRegexError(
        form,
        fieldName,
        "random",
        error = FormError(fieldName, invalidCharactersKey, Seq(validCharacterRegex))
      )
    )
  }
}
