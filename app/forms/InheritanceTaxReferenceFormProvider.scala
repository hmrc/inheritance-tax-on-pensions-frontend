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

class InheritanceTaxReferenceFormProvider @Inject() extends Mappings {

  val regex: Regex = "^[A-Z]\\d{6}/\\d{2}[A-Z]$".r

  def apply(): Form[String] =
    Form(
      "value" -> text("inheritanceTaxReference.error.required")
        .verifying(maxLength(11, "inheritanceTaxReference.error.length"))
        .verifying(regexp(regex.toString(), "inheritanceTaxReference.error.invalid"))
    )
}
