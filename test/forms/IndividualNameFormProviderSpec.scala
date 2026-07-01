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

import models.{IndividualName, JourneyRole}
import play.api.data.FormError

class IndividualNameFormProviderSpec extends forms.behaviours.StringFieldBehaviours {

  private val form = new IndividualNameFormProvider()(JourneyRole.LprIndividual)
  private val organisationForm = new IndividualNameFormProvider()(JourneyRole.LprOrganisation)

  "IndividualNameFormProvider" - {

    "must bind valid data" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors mustBe empty
      result.value mustBe Some(
        IndividualName(
          title = Some("Mr"),
          firstForename = "John",
          secondForename = Some("William"),
          surname = "Doe"
        )
      )
    }

    "must fail when firstForename is blank" in {

      val result = form.bind(Map("firstForename" -> "", "surname" -> "Doe"))

      result.errors must contain(FormError("firstForename", "lprIndividualName.error.firstForename.required"))
    }

    "must fail when surname is blank" in {

      val result = form.bind(Map("firstForename" -> "John", "surname" -> ""))

      result.errors must contain(FormError("surname", "lprIndividualName.error.surname.required"))
    }

    "must fail when fields exceed the maximum length" in {

      val result = form.bind(
        Map(
          "title" -> "Title",
          "firstForename" -> ("A" * 36),
          "secondForename" -> ("A" * 36),
          "surname" -> ("A" * 36)
        )
      )

      result.errors must contain(FormError("title", "lprIndividualName.error.title.length", Seq(4)))
      result.errors must contain(FormError("firstForename", "lprIndividualName.error.firstForename.length", Seq(35)))
      result.errors must contain(FormError("secondForename", "lprIndividualName.error.secondForename.length", Seq(35)))
      result.errors must contain(FormError("surname", "lprIndividualName.error.surname.length", Seq(35)))
    }

    "must fail when fields contain invalid characters" in {

      val result = form.bind(
        Map(
          "title" -> "M12",
          "firstForename" -> "John1",
          "secondForename" -> "William1",
          "surname" -> "Doe1"
        )
      )

      (result.errors.map(_.message) must contain).allOf(
        "lprIndividualName.error.title.pattern",
        "lprIndividualName.error.firstForename.pattern",
        "lprIndividualName.error.secondForename.pattern",
        "lprIndividualName.error.surname.pattern"
      )
    }

    "must only return the highest priority error for each field" in {

      val tooLongAndInvalidName = s"${"A" * 36}1"
      val result = form.bind(
        Map(
          "title" -> "Title1",
          "firstForename" -> tooLongAndInvalidName,
          "secondForename" -> tooLongAndInvalidName,
          "surname" -> tooLongAndInvalidName
        )
      )

      result.errors.map(error => error.key -> error.message) mustBe Seq(
        "title" -> "lprIndividualName.error.title.pattern",
        "firstForename" -> "lprIndividualName.error.firstForename.pattern",
        "secondForename" -> "lprIndividualName.error.secondForename.pattern",
        "surname" -> "lprIndividualName.error.surname.pattern"
      )
    }

    "must trim whitespace" in {

      val result = form.bind(
        Map(
          "title" -> " Mr ",
          "firstForename" -> " John ",
          "secondForename" -> " William ",
          "surname" -> " Doe "
        )
      )

      result.errors mustBe empty
      result.value mustBe Some(
        IndividualName(
          title = Some("Mr"),
          firstForename = "John",
          secondForename = Some("William"),
          surname = "Doe"
        )
      )
    }

    "must use organisation PR error keys when firstForename and surname are blank" in {

      val result = organisationForm.bind(Map("firstForename" -> "", "surname" -> ""))

      result.errors must contain(FormError("firstForename", "lprOrganisationName.error.firstForename.required"))
      result.errors must contain(FormError("surname", "lprOrganisationName.error.surname.required"))
    }

    "must use organisation PR error keys when fields exceed the maximum length" in {

      val result = organisationForm.bind(
        Map(
          "title" -> "Title",
          "firstForename" -> ("A" * 36),
          "secondForename" -> ("A" * 36),
          "surname" -> ("A" * 36)
        )
      )

      result.errors must contain(FormError("title", "lprOrganisationName.error.title.length", Seq(4)))
      result.errors must contain(FormError("firstForename", "lprOrganisationName.error.firstForename.length", Seq(35)))
      result.errors must contain(
        FormError("secondForename", "lprOrganisationName.error.secondForename.length", Seq(35))
      )
      result.errors must contain(FormError("surname", "lprOrganisationName.error.surname.length", Seq(35)))
    }

    "must use organisation PR error keys when fields contain invalid characters" in {

      val result = organisationForm.bind(
        Map(
          "title" -> "M12",
          "firstForename" -> "John1",
          "secondForename" -> "William1",
          "surname" -> "Doe1"
        )
      )

      (result.errors.map(_.message) must contain).allOf(
        "lprOrganisationName.error.title.pattern",
        "lprOrganisationName.error.firstForename.pattern",
        "lprOrganisationName.error.secondForename.pattern",
        "lprOrganisationName.error.surname.pattern"
      )
    }
  }
}
