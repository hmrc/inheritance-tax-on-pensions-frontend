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
  private val addresslineRegex = """^[^%$£\r\n]+$"""
  private val form = new PrAddressFormProvider()(country)
  private val validData = Map(
    "addressline1" -> "1 Street Road",
    "addressline2" -> "2 Cathedral Square",
    "addressline3" -> "Newcastle upon Tyne",
    "addressline4" -> "",
    "ukPostcode" -> "NE1 1EH"
  )

  "PrAddressFormProvider" - {

    "must bind valid data, trim the fields and retain the existing country" in {
      val result = form.bind(
        validData.updated("addressline1", "  1 Street Road  ").updated("addressline4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressline1 = "1 Street Road",
        addressline2 = Some("2 Cathedral Square"),
        addressline3 = Some("Newcastle upon Tyne"),
        addressline4 = None,
        ukPostcode = Some("NE1 1EH"),
        country = country
      )
    }

    "must require address line 1" in {
      val result = form.bind(validData.updated("addressline1", "   "))

      result.errors must contain(
        FormError("addressline1", "changePrAddress.error.addressline1.required")
      )
    }

    "must accept characters other than percent, dollar and pound signs" in {
      val result = form.bind(validData.updated("addressline1", "Flat #2: \"Rear\" @ Block_B; [A]?"))

      result.errors mustBe empty
    }

    Seq(
      ("addressline1", "changePrAddress.error.addressline1.invalid"),
      ("addressline2", "changePrAddress.error.addressline2.invalid"),
      ("addressline3", "changePrAddress.error.addressline3.invalid"),
      ("addressline4", "changePrAddress.error.addressline4.invalid"),
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

          result.errors must contain(FormError(field, errorKey, Seq(addresslineRegex)))
        }
      }
    }

    Seq(
      ("addressline1", "changePrAddress.error.addressline1.length"),
      ("addressline2", "changePrAddress.error.addressline2.length"),
      ("addressline3", "changePrAddress.error.addressline3.length"),
      ("addressline4", "changePrAddress.error.addressline4.length"),
      ("ukPostcode", "changePrAddress.error.ukPostcode.length")
    ).foreach { case (field, errorKey) =>
      s"must reject $field when it is longer than 35 characters" in {
        val result = form.bind(validData.updated(field, "A" * 36))

        result.errors must contain(FormError(field, errorKey, Seq(35)))
      }
    }

    "must show only the higher-priority invalid-format error for one field" in {
      val result = form.bind(validData.updated("addressline1", "%" * 36))

      result.errors.filter(_.key == "addressline1") mustBe Seq(
        FormError(
          "addressline1",
          "changePrAddress.error.addressline1.invalid",
          Seq(addresslineRegex)
        )
      )
    }
  }
}
