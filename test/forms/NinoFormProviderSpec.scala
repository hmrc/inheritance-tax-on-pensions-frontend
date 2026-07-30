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
import uk.gov.hmrc.domain.Nino
import play.api.data.FormError

class NinoFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "nino.error.required"
  val invalidCharactersKey = "nino.error.invalid"

  val form = new NinoFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        "AA123456A"
      )
    )

    "must bind valid national insurance number and ignore whitespace and lowercase" in {
      val result = form.bind(Map("value" -> "aa12  3456  A  "))
      result.errors mustBe empty
      result.value mustBe Some(Nino("AA123456A"))
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
