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

package models.addresslookup

import base.SpecBase
import play.api.libs.json.{JsError, JsString, Json}

class AlfAddressDataSpec extends SpecBase {

  "AlfAddressData" - {

    "must read and write json" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = Some("Organisation Name"),
          lines = Seq("33 AB Street", "AB Area"),
          town = Some("ABville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = Some("16651")
        )
      )

      val json = Json.toJson(addressData)

      (json \ "id").as[String] mustBe "GB123"
      (json \ "address" \ "organisation").as[String] mustBe "Organisation Name"
      (json \ "address" \ "lines").as[Seq[String]] mustBe Seq("33 AB Street", "AB Area")
      (json \ "address" \ "town").as[String] mustBe "ABville"
      (json \ "address" \ "postcode").as[String] mustBe "ZZ1 1ZZ"
      (json \ "address" \ "country" \ "code").as[String] mustBe "GB"
      (json \ "address" \ "country" \ "name").as[String] mustBe "United Kingdom"
      (json \ "address" \ "poBox").as[String] mustBe "16651"
      json.as[AlfAddressData] mustBe addressData
    }

    "must fail to read invalid json" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("address" -> "text"),
        Json.obj("address" -> Json.obj()),
        Json.obj("address" -> Json.obj("lines" -> "text")),
        Json.obj("address" -> Json.obj("lines" -> Json.arr("a"), "country" -> "text")),
        Json.obj("address" -> Json.obj("lines" -> "text", "country" -> "text"))
      ).foreach { json =>
        json.validate[AlfAddressData] mustBe a[JsError]
      }
    }

    "must fail to read invalid countries" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("code" -> 1),
        Json.obj("code" -> "GB", "name" -> 1),
        Json.obj("code" -> 1, "name" -> 1)
      ).foreach { json =>
        json.validate[AlfCountry] mustBe a[JsError]
      }
    }
  }
}
