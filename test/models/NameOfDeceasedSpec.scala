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

package models

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class NameOfDeceasedSpec extends SpecBase {

  "NameOfDeceased" - {

    "must successfully read from json" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "John",
        "secondForename" -> "William",
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe JsSuccess(
        NameOfDeceased(
          title = Some("Mr"),
          firstForename = "John",
          secondForename = Some("William"),
          surname = "Doe"
        )
      )
    }

    "must successfully write to json" in {

      val nameOfDeceased = NameOfDeceased(
        title = Some("Mr"),
        firstForename = "John",
        secondForename = Some("William"),
        surname = "Doe"
      )

      val json = Json.toJson(nameOfDeceased)

      (json \ "title").as[String] mustBe "Mr"
      (json \ "firstForename").as[String] mustBe "John"
      (json \ "secondForename").as[String] mustBe "William"
      (json \ "surname").as[String] mustBe "Doe"
    }

    "must handle missing optional fields when reading from json" in {

      val json = Json.obj(
        "firstForename" -> "John",
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe JsSuccess(
        NameOfDeceased(
          title = None,
          firstForename = "John",
          secondForename = None,
          surname = "Doe"
        )
      )
    }

    "must fail when required field firstForename is missing" in {

      val json = Json.obj(
        "title" -> "Mr",
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe a[JsError]
    }

    "must fail when required field surname is missing" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "John"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe a[JsError]
    }

    "must fail when firstForename is wrong type" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> 123,
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe a[JsError]
    }

    "must fail when surname is wrong type" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "John",
        "surname" -> 123
      )

      val result = json.validate[NameOfDeceased]

      result mustBe a[JsError]
    }

    "must handle empty string for optional title" in {

      val json = Json.obj(
        "title" -> "",
        "firstForename" -> "John",
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe JsSuccess(
        NameOfDeceased(
          title = Some(""),
          firstForename = "John",
          secondForename = None,
          surname = "Doe"
        )
      )
    }

    "must handle empty string for optional secondForename" in {

      val json = Json.obj(
        "firstForename" -> "John",
        "secondForename" -> "",
        "surname" -> "Doe"
      )

      val result = json.validate[NameOfDeceased]

      result mustBe JsSuccess(
        NameOfDeceased(
          title = None,
          firstForename = "John",
          secondForename = Some(""),
          surname = "Doe"
        )
      )
    }

    "must write to json with only present fields" in {

      val nameOfDeceased = NameOfDeceased(
        title = None,
        firstForename = "John",
        secondForename = None,
        surname = "Doe"
      )

      val json = Json.toJson(nameOfDeceased)

      (json \ "firstForename").as[String] mustBe "John"
      (json \ "surname").as[String] mustBe "Doe"
      (json \ "title").toOption mustBe None
      (json \ "secondForename").toOption mustBe None
    }
  }
}
