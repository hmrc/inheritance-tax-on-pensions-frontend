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

import org.scalatest.matchers.must.Matchers
import utils.TestCurrencyEntry
import org.scalatest.freespec.AnyFreeSpec

class CurrencyEntryFormProviderSpec extends AnyFreeSpec with Matchers {
  private val provider = new CurrencyEntryFormProvider()
  private val entry = TestCurrencyEntry(messagePrefix = "currencyEntry")
  private val form = provider(entry)

  "CurrencyEntryFormProvider" - {
    "must accept currency formats and the inclusive boundaries" in {
      Seq(
        "0" -> "0",
        "0.01" -> "0.01",
        "1234" -> "1234",
        "1,234.56" -> "1234.56",
        "\u00a31,234.56" -> "1234.56",
        " 1234.50 " -> "1234.50",
        "999999999.99" -> "999999999.99"
      ).foreach { case (input, expected) =>
        withClue(s"input=$input: ") {
          form.bind(Map("value" -> input)).value mustBe Some(BigDecimal(expected))
        }
      }
    }

    "must use the entry's required and invalid message keys" in {
      Seq(Map.empty[String, String], Map("value" -> ""), Map("value" -> "   ")).foreach { data =>
        form.bind(data).errors.map(_.message) mustBe Seq("currencyEntry.error.required")
      }
      Seq("abc", "10%", "&", "12\u00a3", "-1", "1.234", "1e3", ".", "\u00a3").foreach { input =>
        withClue(s"input=$input: ") {
          form.bind(Map("value" -> input)).errors.map(_.message) mustBe Seq("currencyEntry.error.invalid")
        }
      }
    }

    "must use a distinct message above the maximum" in {
      form.bind(Map("value" -> "1000000000")).errors.map(_.message) mustBe Seq("currencyEntry.error.tooHigh")
    }

    "must support entry-specific limits and an existing maximum error key" in {
      val configuredForm = provider(
        entry.copy(
          minimum = BigDecimal(1),
          maximum = BigDecimal(10),
          maximumError = Some("currencyEntry.error.invalid")
        )
      )
      configuredForm.bind(Map("value" -> "0.99")).errors.map(_.message) mustBe Seq("currencyEntry.error.invalid")
      configuredForm.bind(Map("value" -> "10.01")).errors.map(_.message) mustBe Seq("currencyEntry.error.invalid")
      configuredForm.bind(Map("value" -> "10")).value mustBe Some(BigDecimal(10))
    }
  }
}
