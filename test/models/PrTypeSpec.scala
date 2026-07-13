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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class PrTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PrType" - {

    "must read individual and organisation from JSON into PrType values" in {

      val gen = Gen.oneOf(PrType.values.toSeq)

      forAll(gen) { prType =>
        JsString(prType.toString).validate[PrType].asOpt.value mustEqual prType
      }
    }

    "must reject JSON strings other than individual and organisation" in {

      val gen = arbitrary[String].suchThat(!PrType.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[PrType] mustEqual JsError("error.invalid")
      }
    }

    "must write PrType values to JSON strings individual and organisation" in {

      val gen = Gen.oneOf(PrType.values.toSeq)

      forAll(gen) { prType =>
        Json.toJson(prType) mustEqual JsString(prType.toString)
      }
    }
  }
}
