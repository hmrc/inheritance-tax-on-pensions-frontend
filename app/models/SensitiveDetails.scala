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

import uk.gov.hmrc.crypto.Sensitive
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryption

case class SensitiveString(override val decryptedValue: String) extends Sensitive[String] {
  override def equals(obj: Any): Boolean = obj match {
    case other: SensitiveString => this.decryptedValue == other.decryptedValue
    case _ => false
  }

  override def hashCode(): Int = decryptedValue.hashCode
}

case class SensitiveIndividualDetails(
  firstName: SensitiveString,
  middleName: Option[SensitiveString],
  lastName: SensitiveString
) {
  def fullName: String =
    s"${firstName.decryptedValue} ${middleName.map(_.decryptedValue).getOrElse("")} ${lastName.decryptedValue}"
  override def equals(obj: Any): Boolean = obj match {
    case other: SensitiveIndividualDetails =>
      this.firstName.decryptedValue == other.firstName.decryptedValue &&
      this.middleName.map(_.decryptedValue) == other.middleName.map(_.decryptedValue) &&
      this.lastName.decryptedValue == other.lastName.decryptedValue
    case _ => false
  }

  override def hashCode(): Int =
    (firstName.decryptedValue, middleName.map(_.decryptedValue), lastName.decryptedValue).hashCode
}

object SensitiveDetails {

  implicit val sensitiveStringReads: Reads[SensitiveString] =
    Reads.of[String].map(SensitiveString.apply)

  implicit val sensitiveStringWrites: Writes[SensitiveString] =
    Writes(s => JsString(s.decryptedValue))

  implicit val sensitiveIndividualDetailsReads: Reads[SensitiveIndividualDetails] =
    Json.reads[SensitiveIndividualDetails]

  implicit val sensitiveIndividualDetailsWrites: Writes[SensitiveIndividualDetails] =
    Json.writes[SensitiveIndividualDetails]

  implicit def sensitiveStringFormat(implicit
    crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter
  ): Format[SensitiveString] =
    crypto match {
      case _: config.NoOpCrypto.type =>
        Format(sensitiveStringReads, sensitiveStringWrites)
      case _ =>
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    }

  implicit def sensitiveIndividualDetailsFormat(implicit
    crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter
  ): Format[SensitiveIndividualDetails] =
    crypto match {
      case _: config.NoOpCrypto.type =>
        Format(sensitiveIndividualDetailsReads, sensitiveIndividualDetailsWrites)
      case _ =>
        Json.format[SensitiveIndividualDetails]
    }
}
