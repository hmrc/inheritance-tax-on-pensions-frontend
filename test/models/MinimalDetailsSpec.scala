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
import play.api.libs.json.Json

class MinimalDetailsSpec extends SpecBase {

  private val testKey = "teStTesttE5TtesT3TEsTtEsttESTTest5TEsTtE5t1="
  private implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
    SymmetricCryptoFactory.aesGcmCrypto(testKey)

  "MinimalDetails" - {

    "must serialize and deserialize with plain format (API)" in {
      val details = MinimalDetails(
        email = SensitiveString("test@example.com"),
        isPsaSuspended = false,
        organisationName = Some("Test Org"),
        individualDetails = Some(
          SensitiveIndividualDetails(
            firstName = SensitiveString("John"),
            middleName = None,
            lastName = SensitiveString("Doe")
          )
        ),
        rlsFlag = false,
        deceasedFlag = false
      )

      val json = Json.toJson(details)(using MinimalDetails.writes)
      val result = json.as[MinimalDetails](using MinimalDetails.reads)

      result.email.decryptedValue mustBe "test@example.com"
      result.isPsaSuspended mustBe false
      result.organisationName mustBe Some("Test Org")
      result.individualDetails.get.firstName.decryptedValue mustBe "John"
      result.individualDetails.get.lastName.decryptedValue mustBe "Doe"
      result.rlsFlag mustBe false
      result.deceasedFlag mustBe false

      (json \ "email").as[String] mustBe "test@example.com"
    }

    "must serialize and deserialize with encrypted format (MongoDB)" in {
      val details = MinimalDetails(
        email = SensitiveString("test@example.com"),
        isPsaSuspended = false,
        organisationName = Some("Test Org"),
        individualDetails = Some(
          SensitiveIndividualDetails(
            firstName = SensitiveString("John"),
            middleName = None,
            lastName = SensitiveString("Doe")
          )
        ),
        rlsFlag = false,
        deceasedFlag = false
      )

      val json = Json.toJson(details)(using MinimalDetails.encryptedFormat)
      val result = json.as[MinimalDetails](using MinimalDetails.encryptedFormat)

      result.email.decryptedValue mustBe "test@example.com"
      result.isPsaSuspended mustBe false
      result.organisationName mustBe Some("Test Org")
      result.individualDetails.get.firstName.decryptedValue mustBe "John"
      result.individualDetails.get.lastName.decryptedValue mustBe "Doe"

      val emailValue = (json \ "email").as[String]
      (emailValue must not).equal("test@example.com")
      emailValue.length must be > 0

      val individualDetailsValue = Json.stringify((json \ "individualDetails").get)
      individualDetailsValue.length must be > 0
      (individualDetailsValue must not).include("John")
    }

    "must handle organisation without individual details" in {
      val details = MinimalDetails(
        email = SensitiveString("org@example.com"),
        isPsaSuspended = true,
        organisationName = Some("Test Organisation"),
        individualDetails = None,
        rlsFlag = true,
        deceasedFlag = false
      )

      val json = Json.toJson(details)(using MinimalDetails.encryptedFormat)
      val result = json.as[MinimalDetails](using MinimalDetails.encryptedFormat)

      result.email.decryptedValue mustBe "org@example.com"
      result.isPsaSuspended mustBe true
      result.organisationName mustBe Some("Test Organisation")
      result.individualDetails mustBe None
      result.rlsFlag mustBe true
      result.deceasedFlag mustBe false
    }

    "must handle individual without organisation name" in {
      val details = MinimalDetails(
        email = SensitiveString("individual@example.com"),
        isPsaSuspended = false,
        organisationName = None,
        individualDetails = Some(
          SensitiveIndividualDetails(
            firstName = SensitiveString("Jane"),
            middleName = Some(SensitiveString("Marie")),
            lastName = SensitiveString("Doe")
          )
        ),
        rlsFlag = false,
        deceasedFlag = true
      )

      val json = Json.toJson(details)(using MinimalDetails.encryptedFormat)
      val result = json.as[MinimalDetails](using MinimalDetails.encryptedFormat)

      result.email.decryptedValue mustBe "individual@example.com"
      result.organisationName mustBe None
      result.individualDetails.get.firstName.decryptedValue mustBe "Jane"
      result.individualDetails.get.middleName.map(_.decryptedValue) mustBe Some("Marie")
      result.individualDetails.get.lastName.decryptedValue mustBe "Doe"
      result.deceasedFlag mustBe true
    }
  }

  "IndividualDetails" - {

    "must serialize and deserialize" in {
      val details = IndividualDetails(
        firstName = "Jane",
        middleName = Some("Marie"),
        lastName = "Doe"
      )

      val json = Json.toJson(details)(using IndividualDetails.writes)
      val result = json.as[IndividualDetails](using IndividualDetails.reads)

      result.firstName mustBe "Jane"
      result.middleName mustBe Some("Marie")
      result.lastName mustBe "Doe"
    }

    "must generate full name" in {
      val details = IndividualDetails(
        firstName = "Jane",
        middleName = Some("Marie"),
        lastName = "Doe"
      )

      details.fullName mustBe "Jane Marie Doe"
    }

    "must serialize and deserialize without middle name" in {
      val details = IndividualDetails(
        firstName = "Jane",
        middleName = None,
        lastName = "Doe"
      )

      val json = Json.toJson(details)(using IndividualDetails.writes)
      val result = json.as[IndividualDetails](using IndividualDetails.reads)

      result.firstName mustBe "Jane"
      result.middleName mustBe None
      result.lastName mustBe "Doe"
    }
  }
}
