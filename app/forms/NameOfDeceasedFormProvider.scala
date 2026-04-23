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
import play.api.data.Forms.{mapping, optional}
import models.NameOfDeceased
import play.api.data.Form

import javax.inject.Inject

class NameOfDeceasedFormProvider @Inject() extends Mappings {

  def apply(): Form[NameOfDeceased] =
    Form(
      mapping(
        "title" -> optional(
          text()
            .transform[String](input => input.trim, identity)
            .verifying(maxLength(4, "nameOfDeceased.error.title.length"))
            .verifying(regexp(nameRegex, "nameOfDeceased.error.title.pattern"))
        ),
        "firstForename" -> text("nameOfDeceased.error.firstForename.required")
          .transform[String](input => input.trim, identity)
          .verifying(maxLength(35, "nameOfDeceased.error.firstForename.length"))
          .verifying(regexp(nameRegex, "nameOfDeceased.error.firstForename.pattern")),
        "secondForename" -> optional(
          text()
            .transform[String](input => input.trim, identity)
            .verifying(maxLength(35, "nameOfDeceased.error.secondForename.length"))
            .verifying(regexp(nameRegex, "nameOfDeceased.error.secondForename.pattern"))
        ),
        "surname" -> text("nameOfDeceased.error.surname.required")
          .transform[String](input => input.trim, identity)
          .verifying(maxLength(35, "nameOfDeceased.error.surname.length"))
          .verifying(regexp(nameRegex, "nameOfDeceased.error.surname.pattern"))
      )((title, firstForename, secondForename, surname) =>
        NameOfDeceased(title, firstForename, secondForename, surname)
      )(deceased => Some((deceased.title, deceased.firstForename, deceased.secondForename, deceased.surname)))
    )
}
