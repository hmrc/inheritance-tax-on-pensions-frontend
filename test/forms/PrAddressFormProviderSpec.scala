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
import forms.mappings.Regex
import models.PrAddress
import play.api.data.FormError

class PrAddressFormProviderSpec extends AnyFreeSpec with Matchers with Regex {

  private val country = "GB"
  private val nonGbCountry = "BF"
  private val form = new PrAddressFormProvider()(country)
  private val formNonGb = new PrAddressFormProvider()(nonGbCountry)
  private val validData = Map(
    "addressLine1" -> "Line 1",
    "addressLine2" -> "Line 2",
    "addressLine3" -> "City",
    "addressLine4" -> "",
    "addressLine5" -> "Line 5",
    "postCode" -> ""
  )
  "PrAddressFormProvider" - {

    "must bind valid data, trim the fields and retain the existing country" in {
      val result = form.bind(
        validData.updated("addressLine1", "  Line 1  ").updated("addressLine4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressLine1 = "Line 1",
        addressLine2 = Some("Line 2"),
        addressLine3 = Some("City"),
        addressLine4 = None,
        addressLine5 = Some("Line 5"),
        postCode = None,
        country = country
      )
    }
    "must bind valid data, trim the fields and retain the existing Non-GB country" in {
      val result = formNonGb.bind(
        validData.updated("addressLine1", "  Line 1  ").updated("addressLine4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressLine1 = "Line 1",
        addressLine2 = Some("Line 2"),
        addressLine3 = Some("City"),
        addressLine4 = None,
        addressLine5 = Some("Line 5"),
        postCode = None,
        country = nonGbCountry
      )
    }

    "must require address line 1" in {
      val result = form.bind(validData.updated("addressLine1", "   "))

      result.errors must contain(
        FormError("addressLine1", "changePrAddress.error.addressLine1.required")
      )
    }

    "must accept characters other than percent, dollar, ampersand and pound signs" in {
      val result = form.bind(validData.updated("addressLine1", "Flat #2: Rear&Co @ Block_B; [A]?"))

      result.errors mustBe empty
    }

    Seq(
      ("addressLine1", "changePrAddress.error.addressLine1.invalid"),
      ("addressLine2", "changePrAddress.error.addressLine2.invalid"),
      ("addressLine3", "changePrAddress.error.addressLine3.invalid"),
      ("addressLine4", "changePrAddress.error.addressLine4.invalid")
    ).foreach { case (field, errorKey) =>
      Seq(
        "%" -> "percent sign",
        "$" -> "dollar sign",
        "£" -> "pound sign",
        "\r" -> "carriage return",
        "\n" -> "newline",
        "\"" -> "quote"
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
      ("addressLine4", "changePrAddress.error.addressLine4.length")
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

    Seq(
      "postcode%",
      "postcode",
      "INVALID",
      "12345",
      "SW1A",
      "SW1A 2A",
      "SW1A 2AAA",
      "T11YEE0"
    ).foreach { postcode =>
      s"must reject invalid UK postcode $postcode" in {
        val result = form.bind(validData.updated("postCode", postcode))
        result.errors must not be empty
        result.errors.exists(_.message == "changePrAddress.error.postCode.invalid") mustBe true
      }
    }

    Seq(
      "AB1 1BA",
      "AB11BA",
      "ab1 1ba",
      "ab11ba",
      "ab121ba",
      "GIR 0AA",
      "gir0aa",
      "FX11XX",
      "W12DN",
      "DE128HJ"
    ).foreach { postcode =>
      s"must accept valid UK postcode $postcode" in {
        val result = form.bind(validData.updated("postCode", postcode))
        result.errors mustBe empty
      }
    }
  }
}
