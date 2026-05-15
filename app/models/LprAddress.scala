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

import models.addresslookup.AlfAddressData
import play.api.libs.json.{Json, OFormat}

final case class LprAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  ukPostcode: Option[String],
  country: String
)

object LprAddress {
  implicit val format: OFormat[LprAddress] = Json.format[LprAddress]

  def hasValidFirstAddressLine(addressData: AlfAddressData): Boolean =
    addressLines(addressData).headOption.exists(_.trim.nonEmpty)

  def fromAlfAddressData(addressData: AlfAddressData): LprAddress = {
    val lines = addressLines(addressData)
    val addressLine2 = lines.lift(1)

    LprAddress(
      addressLine1 = lines.headOption.map(_.trim).getOrElse(""),
      addressLine2 = addressLine2.orElse(addressData.address.town),
      addressLine3 = lines.lift(2),
      addressLine4 = lines.lift(3).orElse(addressData.address.town.filter(_ => addressLine2.isDefined)),
      ukPostcode = addressData.address.postcode,
      country = addressData.address.country.code
    )
  }

  private def addressLines(addressData: AlfAddressData): Seq[String] = {
    val lines = addressData.address.lines.map(_.trim).filter(_.nonEmpty)

    addressData.address.poBox.map(formatPoBox).filterNot(poBox => lines.exists(line => samePoBox(line, poBox))) match {
      case Some(poBox) => poBox +: lines
      case None => lines
    }
  }

  private def formatPoBox(poBox: String): String = {
    val formattedPoBox = poBox.trim

    if (formattedPoBox.toLowerCase.startsWith("po box")) {
      formattedPoBox
    } else {
      s"PO Box $formattedPoBox"
    }
  }

  private def samePoBox(line: String, poBox: String): Boolean =
    line.replaceAll("\\s+", "").equalsIgnoreCase(poBox.replaceAll("\\s+", ""))
}
