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

package config

import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import play.api.Configuration

import javax.inject.Inject

class EncryptedFormats @Inject() (config: Configuration) {
  private val encryptionEnabled = config.getOptional[Boolean]("mongodb.encryption.enabled").getOrElse(false)

  implicit lazy val crypto: Encrypter & Decrypter =
    if (encryptionEnabled)
      SymmetricCryptoFactory.aesGcmCryptoFromConfig("mongodb.encryption", config.underlying)
    else
      NoOpCrypto
}

object NoOpCrypto extends Encrypter with Decrypter {
  override def encrypt(plain: uk.gov.hmrc.crypto.PlainContent): uk.gov.hmrc.crypto.Crypted =
    plain match {
      case uk.gov.hmrc.crypto.PlainText(value) => uk.gov.hmrc.crypto.Crypted(value)
      case uk.gov.hmrc.crypto.PlainBytes(bytes) => uk.gov.hmrc.crypto.Crypted(new String(bytes))
    }

  override def decrypt(crypted: uk.gov.hmrc.crypto.Crypted): uk.gov.hmrc.crypto.PlainText =
    uk.gov.hmrc.crypto.PlainText(crypted.value)

  override def decryptAsBytes(reversiblyEncrypted: uk.gov.hmrc.crypto.Crypted): uk.gov.hmrc.crypto.PlainBytes =
    uk.gov.hmrc.crypto.PlainBytes(reversiblyEncrypted.value.getBytes)
}
