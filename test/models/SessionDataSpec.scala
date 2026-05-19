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
import play.api.libs.json.{JsError, Json}

class SessionDataSpec extends SpecBase {

  "SessionData" - {

    "must deserialize with Administrator" in {
      val json = Json.obj("administratorOrPractitioner" -> "administrator")
      val result = json.as[SessionData]

      result.administratorOrPractitioner mustBe PensionSchemeUser.Administrator
    }

    "must deserialize with Practitioner" in {
      val json = Json.obj("administratorOrPractitioner" -> "practitioner")
      val result = json.as[SessionData]

      result.administratorOrPractitioner mustBe PensionSchemeUser.Practitioner
    }
  }

  "PensionSchemeUser" - {

    "must read Administrator from JSON" in {
      val json = Json.obj("administratorOrPractitioner" -> "administrator")
      val result = json.as[SessionData]

      result.administratorOrPractitioner mustBe PensionSchemeUser.Administrator
    }

    "must read Practitioner from JSON" in {
      val json = Json.obj("administratorOrPractitioner" -> "practitioner")
      val result = json.as[SessionData]

      result.administratorOrPractitioner mustBe PensionSchemeUser.Practitioner
    }

    "must return JsError for unknown value" in {
      val json = Json.obj("administratorOrPractitioner" -> "unknown")
      val result = json.validate[SessionData]

      result mustBe a[JsError]
    }
  }
}
