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

final case class PrAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: String,
  postCode: Option[String],
  country: String,
  addressLine5: Option[String] = None
)

object PrAddress {
  implicit val format: OFormat[PrAddress] = Json.format[PrAddress]

  def hasValidFirstAddressLine(addressData: AlfAddressData): Boolean =
    addressLines(addressData).headOption.exists(_.trim.nonEmpty)

  def fromAlfAddressData(addressData: AlfAddressData): PrAddress = {
    val rawLines = addressLines(addressData)
    val (town, lines) = addressData.address.town.map(_.trim).filter(_.nonEmpty) match {
      case Some(t) => (t, rawLines)
      case None => (rawLines.lastOption.map(_.trim).filter(_.nonEmpty).getOrElse(""), rawLines.dropRight(1))
    }

    PrAddress(
      addressLine1 = lines.headOption.map(_.trim).getOrElse(""),
      addressLine2 = lines.lift(1),
      addressLine3 = lines.lift(2),
      addressLine4 = town,
      addressLine5 = addressData.address.country.code match {
        case "GB" => None
        case _ => addressData.address.postcode
      },
      postCode = addressData.address.country.code match {
        case "GB" => addressData.address.postcode
        case _ => None
      },
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
    sameAddressLine(line, poBox)

  private def sameAddressLine(left: String, right: String): Boolean =
    left.replaceAll("\\s+", "").equalsIgnoreCase(right.replaceAll("\\s+", ""))
}
