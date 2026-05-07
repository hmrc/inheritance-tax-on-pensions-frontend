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

package services

import base.SpecBase
import play.api.Environment

class CountryServiceSpec extends SpecBase {

  private val service = new CountryService(Environment.simple())

  "CountryService" - {
    "must return a country by code" in {

      service.findByCode("GB").value.name mustBe "United Kingdom"
      service.findByCode("FR").value.name mustBe "France"
      service.findByCode("XK").value.name mustBe "Kosovo"
    }

    "must return a country name for a code" in {

      service.nameForCode("GB") mustBe "United Kingdom"
    }

    "must fall back to the country code when the code is not found" in {

      service.findByCode("XX") mustBe None
      service.nameForCode("XX") mustBe "XX"
    }
  }
}
