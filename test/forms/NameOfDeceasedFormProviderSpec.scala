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

import models.NameOfDeceased
import play.api.data.FormError

class NameOfDeceasedFormProviderSpec extends forms.behaviours.StringFieldBehaviours {

  val form = new NameOfDeceasedFormProvider()()

  "NameOfDeceasedFormProvider" - {

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
        NameOfDeceased(
          title = Some("Mr"),
          firstForename = "John",
          secondForename = Some("William"),
          surname = "Doe"
        )
      )
    }

    "must fail when firstForename is blank" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(FormError("firstForename", "nameOfDeceased.error.firstForename.required"))
    }

    "must fail when surname is blank" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> ""
      )

      val result = form.bind(data)

      result.errors must contain(FormError("surname", "nameOfDeceased.error.surname.required"))
    }

    "must fail when title exceeds 4 characters" in {

      val data = Map(
        "title" -> "Title",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(FormError("title", "nameOfDeceased.error.title.length", Seq(4)))
    }

    "must fail when firstForename exceeds 35 characters" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "A" * 36,
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(FormError("firstForename", "nameOfDeceased.error.firstForename.length", Seq(35)))
    }

    "must fail when secondForename exceeds 35 characters" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "A" * 36,
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(FormError("secondForename", "nameOfDeceased.error.secondForename.length", Seq(35)))
    }

    "must fail when surname exceeds 35 characters" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "A" * 36
      )

      val result = form.bind(data)

      result.errors must contain(FormError("surname", "nameOfDeceased.error.surname.length", Seq(35)))
    }

    "must fail when title has invalid pattern" in {

      val data = Map(
        "title" -> "M12",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError(
          "title",
          "nameOfDeceased.error.title.pattern",
          Seq("^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$")
        )
      )
    }

    "must fail when firstForename has invalid pattern" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John1",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError(
          "firstForename",
          "nameOfDeceased.error.firstForename.pattern",
          Seq("^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$")
        )
      )
    }

    "must fail when secondForename has invalid pattern" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William1",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError(
          "secondForename",
          "nameOfDeceased.error.secondForename.pattern",
          Seq("^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$")
        )
      )
    }

    "must fail when surname has invalid pattern" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "Doe1"
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError(
          "surname",
          "nameOfDeceased.error.surname.pattern",
          Seq("^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$")
        )
      )
    }

    "must trim whitespace" in {

      val data = Map(
        "title" -> " Mr ",
        "firstForename" -> " John ",
        "secondForename" -> " William ",
        "surname" -> " Doe"
      )

      val result = form.bind(data)

      result.errors mustBe empty
      result.value mustBe Some(
        NameOfDeceased(
          title = Some("Mr"),
          firstForename = "John",
          secondForename = Some("William"),
          surname = "Doe"
        )
      )
    }

    "must work with optional fields missing" in {

      val data = Map(
        "firstForename" -> "John",
        "surname" -> "Doe"
      )

      val result = form.bind(data)

      result.errors mustBe empty
      result.value mustBe Some(
        NameOfDeceased(
          title = None,
          firstForename = "John",
          secondForename = None,
          surname = "Doe"
        )
      )
    }
  }
}
