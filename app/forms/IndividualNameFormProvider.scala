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
import models.{IndividualName, JourneyRole}
import play.api.data.Form

import javax.inject.Inject

class IndividualNameFormProvider @Inject() extends Mappings {

  def apply(journeyRole: JourneyRole): Form[IndividualName] =
    Form(
      mapping(
        "title" -> optional(
          text()
            .transform[String](input => input.trim, identity)
            .verifying(
              firstError(
                regexp(nameRegex, s"${journeyRole.key}.error.title.pattern"),
                maxLength(4, s"${journeyRole.key}.error.title.length")
              )
            )
        ),
        "firstForename" -> text(s"${journeyRole.key}.error.firstForename.required")
          .transform[String](input => input.trim, identity)
          .verifying(
            firstError(
              regexp(nameRegex, s"${journeyRole.key}.error.firstForename.pattern"),
              maxLength(35, s"${journeyRole.key}.error.firstForename.length")
            )
          ),
        "secondForename" -> optional(
          text()
            .transform[String](input => input.trim, identity)
            .verifying(
              firstError(
                regexp(nameRegex, s"${journeyRole.key}.error.secondForename.pattern"),
                maxLength(35, s"${journeyRole.key}.error.secondForename.length")
              )
            )
        ),
        "surname" -> text(s"${journeyRole.key}.error.surname.required")
          .transform[String](input => input.trim, identity)
          .verifying(
            firstError(
              regexp(nameRegex, s"${journeyRole.key}.error.surname.pattern"),
              maxLength(35, s"${journeyRole.key}.error.surname.length")
            )
          )
      )((title, firstForename, secondForename, surname) =>
        IndividualName(title, firstForename, secondForename, surname)
      )(name => Some((name.title, name.firstForename, name.secondForename, name.surname)))
    )
}
