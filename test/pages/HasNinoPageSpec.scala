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

import base.SpecBase
import play.api.libs.json.JsPath

class HasNinoPageSpec extends SpecBase {

  "HasNinoPage" - {

    "must use the correct path" in {
      HasNinoPage.path mustEqual JsPath \ "hasNino"
    }

    "must use the correct page name" in {
      HasNinoPage.toString mustEqual "hasNino"
    }

    "must call super.cleanup when value is None" in {
      val userAnswers = emptyUserAnswers

      val result = HasNinoPage.cleanup(None, userAnswers)

      result.success.value mustEqual userAnswers
    }

    "must remove no Nino reason when Nino is selected" in {
      val userAnswers = emptyUserAnswers
        .set(NoNinoReasonPage, "Test reason")
        .success
        .value

      val result = HasNinoPage.cleanup(Some(true), userAnswers).success.value

      result.get(NoNinoReasonPage) mustBe None
    }

    "must remove Nino when no Nino is selected" in {
      val userAnswers = emptyUserAnswers
        .set(NinoPage, "Test Nino")
        .success
        .value

      val result = HasNinoPage.cleanup(Some(false), userAnswers).success.value

      result.get(NinoPage) mustBe None
    }
  }
}
