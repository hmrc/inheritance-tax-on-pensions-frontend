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
import play.api.data.FormError

class IhtPayableFormProviderSpec extends AnyFreeSpec with Matchers {
  private val form = new IhtPayableFormProvider()()

  "IhtPayableFormProvider" - {
    Seq(
      "0" -> "0",
      "0.00" -> "0.00",
      "0.01" -> "0.01",
      "1234" -> "1234",
      "1,234.56" -> "1234.56",
      "\u00a31,234.56" -> "1234.56",
      " 1234.50 " -> "1234.50",
      "999999999.99" -> "999999999.99"
    ).foreach { case (input, expected) =>
      s"must bind $input" in {
        form.bind(Map("value" -> input)).value mustBe Some(BigDecimal(expected))
      }
    }

    Seq(Map.empty[String, String], Map("value" -> ""), Map("value" -> "   ")).foreach { data =>
      s"must require an amount for $data" in {
        form.bind(data).errors mustBe Seq(FormError("value", "ihtPayable.error.required"))
      }
    }

    Seq("abc", "%", "&", "12\u00a3", "-1", "-0.01", "\u00a3-0.01", "1.234", "1e3", ".", "\u00a3", "1000000000")
      .foreach { input =>
        s"must reject $input" in {
          form.bind(Map("value" -> input)).errors.map(_.message) mustBe Seq("ihtPayable.error.invalid")
        }
      }

    "must fill a saved amount" in {
      form.fill(BigDecimal("1234.56"))("value").value mustBe Some("1234.56")
    }
  }
}
