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

package forms

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import models.PrAddress
import play.api.data.FormError

class PrAddressFormProviderSpec extends AnyFreeSpec with Matchers {

  private val country = "GB"
  private val addressLineRegex = """^[^%$£\r\n]+$"""
  private val form = new PrAddressFormProvider()(country)
  private val validData = Map(
    "addressLine1" -> "1 Street Road",
    "addressLine2" -> "2 Cathedral Square",
    "addressLine3" -> "Newcastle upon Tyne",
    "addressLine4" -> "",
    "ukPostcode" -> "NE1 1EH"
  )

  "PrAddressFormProvider" - {

    "must bind valid data, trim the fields and retain the existing country" in {
      val result = form.bind(
        validData.updated("addressLine1", "  1 Street Road  ").updated("addressLine4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressLine1 = "1 Street Road",
        addressLine2 = Some("2 Cathedral Square"),
        addressLine3 = Some("Newcastle upon Tyne"),
        addressLine4 = None,
        ukPostcode = Some("NE1 1EH"),
        country = country
      )
    }

    "must require address line 1" in {
      val result = form.bind(validData.updated("addressLine1", "   "))

      result.errors must contain(
        FormError("addressLine1", "changePrAddress.error.addressLine1.required")
      )
    }

    "must accept characters other than percent, dollar and pound signs" in {
      val result = form.bind(validData.updated("addressLine1", "Flat #2: \"Rear\" @ Block_B; [A]?"))

      result.errors mustBe empty
    }

    Seq(
      ("addressLine1", "changePrAddress.error.addressLine1.invalid"),
      ("addressLine2", "changePrAddress.error.addressLine2.invalid"),
      ("addressLine3", "changePrAddress.error.addressLine3.invalid"),
      ("addressLine4", "changePrAddress.error.addressLine4.invalid"),
      ("ukPostcode", "changePrAddress.error.ukPostcode.invalid")
    ).foreach { case (field, errorKey) =>
      Seq(
        "%" -> "percent sign",
        "$" -> "dollar sign",
        "£" -> "pound sign",
        "\r" -> "carriage return",
        "\n" -> "newline"
      ).foreach { case (invalidCharacter, description) =>
        s"must reject a $description in $field" in {
          val result = form.bind(validData.updated(field, s"Invalid${invalidCharacter}Value"))

          result.errors must contain(FormError(field, errorKey, Seq(addressLineRegex)))
        }
      }
    }

    Seq(
      ("addressLine1", "changePrAddress.error.addressLine1.length"),
      ("addressLine2", "changePrAddress.error.addressLine2.length"),
      ("addressLine3", "changePrAddress.error.addressLine3.length"),
      ("addressLine4", "changePrAddress.error.addressLine4.length"),
      ("ukPostcode", "changePrAddress.error.ukPostcode.length")
    ).foreach { case (field, errorKey) =>
      s"must reject $field when it is longer than 35 characters" in {
        val result = form.bind(validData.updated(field, "A" * 36))

        result.errors must contain(FormError(field, errorKey, Seq(35)))
      }
    }

    "must show only the higher-priority invalid-format error for one field" in {
      val result = form.bind(validData.updated("addressLine1", "%" * 36))

      result.errors.filter(_.key == "addressLine1") mustBe Seq(
        FormError(
          "addressLine1",
          "changePrAddress.error.addressLine1.invalid",
          Seq(addressLineRegex)
        )
      )
    }
  }
}
