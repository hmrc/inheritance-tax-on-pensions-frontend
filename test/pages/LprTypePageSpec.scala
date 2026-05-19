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

package pages

import org.scalatest.freespec.AnyFreeSpec
import base.SpecBase
import play.api.libs.json.JsPath

class LprTypePageSpec extends SpecBase {

  "LprTypePage" - {

    "must use the correct path" in {
      LprTypePage.path mustEqual JsPath \ "lprType"
    }

    "must use the correct page name" in {
      LprTypePage.toString mustEqual "lprType"
    }

    "must call super.cleanup when value is None" in {
      val userAnswers = emptyUserAnswers

      val result = LprTypePage.cleanup(None, userAnswers)

      result.isSuccess mustBe true
    }
  }
}
