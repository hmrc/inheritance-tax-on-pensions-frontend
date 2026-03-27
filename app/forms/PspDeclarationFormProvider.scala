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

import forms.mappings.Mappings
import play.api.data.Form

import scala.util.matching.Regex

import javax.inject.Inject

class PspDeclarationFormProvider @Inject() extends Mappings {

  private val schemeAdminIdRegex: Regex = "^(A[0-9]{7})$".r

  private def sanitiseSchemeAdminId(schemeAdminId: String) =
    if (schemeAdminId.matches(schemeAdminIdRegex.regex)) {
      schemeAdminId
    } else {
      schemeAdminId.replaceAll("\\s+", "").toUpperCase
    }

  def apply(authorisingPSAID: Option[String]): Form[String] =
    Form(
      "value" -> text("pspDeclaration.schemeAdminId.error.required")
        .transform[String](sanitiseSchemeAdminId, identity)
        .verifying(regexp(schemeAdminIdRegex.regex, "pspDeclaration.schemeAdminId.error.invalid"))
        .verifying(isEqual(authorisingPSAID, "pspDeclaration.schemeAdminId.error.noMatch"))
    )
}
