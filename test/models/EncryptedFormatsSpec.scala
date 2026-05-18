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

import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.crypto.PlainText
import config.{EncryptedFormats, NoOpCrypto}
import base.SpecBase
import play.api.Configuration

class EncryptedFormatsSpec extends SpecBase {

  "EncryptedFormats" - {

    "must create real crypto when encryption is enabled" in {
      val config = Configuration(
        ConfigFactory.parseString(
          """
            |mongodb.encryption.enabled = true
            |mongodb.encryption.key = "teStTesttE5TtesT3TEsTtEsttESTTest5TEsTtE5t1="
            |""".stripMargin
        )
      )

      val encryptedFormats = new EncryptedFormats(config)
      val encrypted = encryptedFormats.crypto.encrypt(PlainText("Doe"))

      encrypted.value must not be "Doe"
      encrypted.value must not be empty

      encryptedFormats.crypto.decrypt(encrypted).value mustBe "Doe"
    }

    "must create NoOpCrypto when encryption is disabled" in {
      val config = Configuration(
        ConfigFactory.parseString(
          """
            |mongodb.encryption.enabled = false
            |""".stripMargin
        )
      )

      val encryptedFormats = new EncryptedFormats(config)
      encryptedFormats.crypto mustBe NoOpCrypto
    }

    "must default to NoOpCrypto when encryption config is missing" in {
      val config = Configuration(ConfigFactory.empty())

      val encryptedFormats = new EncryptedFormats(config)
      encryptedFormats.crypto mustBe NoOpCrypto
    }
  }

  "NoOpCrypto" - {

    "must pass through values unchanged on encrypt" in {
      val plainText = PlainText("john doe")
      val encrypted = NoOpCrypto.encrypt(plainText)

      encrypted.value mustBe "john doe"
    }

    "must pass through values unchanged on decrypt" in {
      val crypted = uk.gov.hmrc.crypto.Crypted("john doe")
      val decrypted = NoOpCrypto.decrypt(crypted)

      decrypted.value mustBe "john doe"
    }

    "must handle decryptAsBytes" in {
      val crypted = uk.gov.hmrc.crypto.Crypted("Doe")
      val bytes = NoOpCrypto.decryptAsBytes(crypted)

      new String(bytes.value) mustBe "Doe"
    }

    "must pass through values unchanged on encrypt with PlainBytes" in {
      val plainBytes = uk.gov.hmrc.crypto.PlainBytes("john doe".getBytes)
      val encrypted = NoOpCrypto.encrypt(plainBytes)

      encrypted.value mustBe "john doe"
    }
  }
}
