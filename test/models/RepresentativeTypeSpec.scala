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

class RepresentativeTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "RepresentativeType" - {

    "must use the correct codes for PR and PPR" in {
      RepresentativeType.PR.toString mustEqual "02"
      RepresentativeType.PPR.toString mustEqual "01"
    }

    "must read 01 and 02 from JSON into RepresentativeType values" in {

      val gen = Gen.oneOf(RepresentativeType.values.toSeq)

      forAll(gen) { representativeType =>
        JsString(representativeType.toString).validate[RepresentativeType].asOpt.value mustEqual representativeType
      }
    }

    "must reject JSON strings other than 01 and 02" in {

      val gen = arbitrary[String].suchThat(!RepresentativeType.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[RepresentativeType] mustEqual JsError("error.invalid")
      }
    }

    "must write RepresentativeType values to JSON strings 01 and 02" in {

      val gen = Gen.oneOf(RepresentativeType.values.toSeq)

      forAll(gen) { representativeType =>
        Json.toJson(representativeType) mustEqual JsString(representativeType.toString)
      }
    }
  }
}
