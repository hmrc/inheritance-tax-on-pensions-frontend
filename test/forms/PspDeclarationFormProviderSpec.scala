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
import base.TestValues
import play.api.data.FormError

class PspDeclarationFormProviderSpec extends StringFieldBehaviours with TestValues {

  val requiredKey = "pspDeclaration.schemeAdminId.error.required"
  val invalidCharactersKey = "pspDeclaration.schemeAdminId.error.invalid"
  val noMatchKey = "pspDeclaration.schemeAdminId.error.noMatch"
  val validCharacterRegex = "^(A[0-9]{7})$"

  val form = new PspDeclarationFormProvider()(Some(testPsaId))

  ".value" - {

    val fieldName = "value"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        testPsaId
      )
    )

    "must bind valid SchemeAdminId and ignore whitespace and lowercase" in {
      val result = form.bind(Map("value" -> testPsaId.grouped(2).mkString("  ").toLowerCase(java.util.Locale.ROOT)))
      result.errors mustBe empty
      result.value mustBe Some(testPsaId)
    }

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

    "not bind strings where the SchemeAdminId does not match" in {
      val result = form.bind(Map("value" -> otherPsaId))
      result.errors mustBe Seq(FormError(fieldName, noMatchKey))
    }
  }
}
