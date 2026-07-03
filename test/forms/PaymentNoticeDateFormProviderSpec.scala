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

import play.api.test.Helpers.stubMessages
import base.SpecBase
import play.api.i18n.Messages
import play.api.data.FormError

import java.time.{LocalDate, ZoneOffset}

class PaymentNoticeDateFormProviderSpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()
  private val formProvider = new PaymentNoticeDateFormProvider()
  private def form = formProvider()
  private val fieldKey = "dateThePensionSchemeReceivedNoticeToPay"
  private val validDate = LocalDate.now(ZoneOffset.UTC).minusDays(1)
  private val dateOfDeath = LocalDate.of(2026, 1, 1)

  private def dateData(date: LocalDate): Map[String, String] =
    Map(
      s"$fieldKey.day" -> date.getDayOfMonth.toString,
      s"$fieldKey.month" -> date.getMonthValue.toString,
      s"$fieldKey.year" -> date.getYear.toString
    )

  ".bind" - {

    "must bind a valid date" in {
      val result = formProvider.validate(form.bind(dateData(validDate)))

      result.value.value mustEqual validDate
    }

    "must bind a date when month names and whitespace are entered" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> " 1 ",
            s"$fieldKey.month" -> " Jan uary ",
            s"$fieldKey.year" -> " 2 026 "
          )
        )
      )

      result.value.value mustEqual LocalDate.of(2026, 1, 1)
    }

    "must error when the date fields are empty" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> "",
            s"$fieldKey.month" -> "",
            s"$fieldKey.year" -> ""
          )
        )
      )

      result.errors must contain(FormError(fieldKey, "paymentNoticeDate.error.required.all"))
    }

    "must error when one date field is missing" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> "",
            s"$fieldKey.month" -> "1",
            s"$fieldKey.year" -> "2026"
          )
        )
      )

      result.errors must contain(
        FormError(fieldKey, "paymentNoticeDate.error.required", Seq(messages("date.error.day")))
      )
    }

    "must error when two date fields are missing" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> "",
            s"$fieldKey.month" -> "",
            s"$fieldKey.year" -> "2026"
          )
        )
      )

      result.errors must contain(
        FormError(
          fieldKey,
          "paymentNoticeDate.error.required.two",
          Seq(messages("date.error.day"), messages("date.error.month"))
        )
      )
    }

    "must error when the date is invalid" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> "1",
            s"$fieldKey.month" -> "14",
            s"$fieldKey.year" -> "2026"
          )
        )
      )

      result.errors must contain(FormError(fieldKey, "paymentNoticeDate.error.invalid"))
    }

    "must error when the date is before the date of death" in {
      val result = formProvider.validate(form.bind(dateData(dateOfDeath.minusDays(1))), Some(dateOfDeath))

      result.errors must contain(
        FormError(fieldKey, "paymentNoticeDate.error.afterDateOfDeath", Seq("01/01/2026"))
      )
    }

    "must error when the date is the same as the date of death" in {
      val result = formProvider.validate(form.bind(dateData(dateOfDeath)), Some(dateOfDeath))

      result.errors must contain(
        FormError(fieldKey, "paymentNoticeDate.error.afterDateOfDeath", Seq("01/01/2026"))
      )
    }

    "must bind a date after the date of death" in {
      val result = formProvider.validate(form.bind(dateData(dateOfDeath.plusDays(1))), Some(dateOfDeath))

      result.value.value mustEqual dateOfDeath.plusDays(1)
    }

    "must error when the date is not in the past" in {
      val today = LocalDate.now(ZoneOffset.UTC)
      val result = formProvider.validate(form.bind(dateData(today)))

      result.errors must contain(FormError(fieldKey, "paymentNoticeDate.error.past"))
    }

    "must show the invalid date error before the past date error" in {
      val result = formProvider.validate(
        form.bind(
          Map(
            s"$fieldKey.day" -> "33",
            s"$fieldKey.month" -> "13",
            s"$fieldKey.year" -> "2999"
          )
        )
      )

      result.errors must contain(FormError(fieldKey, "paymentNoticeDate.error.invalid"))
      result.errors must not contain FormError(fieldKey, "paymentNoticeDate.error.past")
    }
  }
}
