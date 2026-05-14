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

import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import base.SpecBase
import play.api.libs.json.{JsString, Json}

class SensitiveDetailsSpec extends SpecBase {

  private val testKey = "teStTesttE5TtesT3TEsTtEsttESTTest5TEsTtE5t1="
  private implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
    SymmetricCryptoFactory.aesGcmCrypto(testKey)

  "SensitiveString" - {

    "must serialize and deserialize with plain format" in {
      val sensitiveString = SensitiveString("test@example.com")
      val json = Json.toJson(sensitiveString)(using SensitiveDetails.sensitiveStringWrites)
      val result = json.as[SensitiveString](using SensitiveDetails.sensitiveStringReads)

      result.decryptedValue mustBe "test@example.com"
      json mustBe JsString("test@example.com")
    }

    "must serialize and deserialize with encrypted format" in {
      val sensitiveString = SensitiveString("test@example.com")
      val json = Json.toJson(sensitiveString)(using SensitiveDetails.sensitiveStringFormat)
      val result = json.as[SensitiveString](using SensitiveDetails.sensitiveStringFormat)

      result.decryptedValue mustBe "test@example.com"
      (json.as[String] must not).equal("test@example.com")
    }

    "must encrypt different values differently" in {
      val string1 = SensitiveString("jane doe")
      val string2 = SensitiveString("john doe")

      val json1 = Json.toJson(string1)(using SensitiveDetails.sensitiveStringFormat)
      val json2 = Json.toJson(string2)(using SensitiveDetails.sensitiveStringFormat)

      (json1 must not).equal(json2)
    }
  }

  "SensitiveIndividualDetails" - {

    "must serialize and deserialize with plain format" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("John"),
        middleName = Some(SensitiveString("Robert")),
        lastName = SensitiveString("Doe")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsWrites)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsReads)

      result.firstName.decryptedValue mustBe "John"
      result.middleName.map(_.decryptedValue) mustBe Some("Robert")
      result.lastName.decryptedValue mustBe "Doe"
    }

    "must serialize and deserialize with encrypted format" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("John"),
        middleName = Some(SensitiveString("Robert")),
        lastName = SensitiveString("Doe")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsFormat)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsFormat)

      result.firstName.decryptedValue mustBe "John"
      result.middleName.map(_.decryptedValue) mustBe Some("Robert")
      result.lastName.decryptedValue mustBe "Doe"

      val jsonString = Json.stringify(json)
      jsonString must not be empty
      (jsonString must not).include("John")
      (jsonString must not).include("Robert")
      (jsonString must not).include("Doe")
    }

    "must handle missing middle name" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("John"),
        middleName = None,
        lastName = SensitiveString("Doe")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsFormat)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsFormat)

      result.firstName.decryptedValue mustBe "John"
      result.middleName mustBe None
      result.lastName.decryptedValue mustBe "Doe"
    }
  }
}
