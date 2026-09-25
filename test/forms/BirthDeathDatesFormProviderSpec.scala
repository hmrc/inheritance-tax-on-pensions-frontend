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

import config.FrontendAppConfig
import base.SpecBase
import models.BirthDeathDates
import play.api.i18n.Messages
import play.api.data.FormError
import play.api.test.Helpers.stubMessages
import org.mockito.Mockito.when

import java.time.LocalDate

class BirthDeathDatesFormProviderSpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()
  private def providerWithMinimum(minimum: LocalDate): BirthDeathDatesFormProvider = {
    val config = mock[FrontendAppConfig]
    when(config.earliestDateOfDeath).thenReturn(minimum)
    new BirthDeathDatesFormProvider(config)
  }

  private val formProvider = providerWithMinimum(LocalDate.of(1900, 2, 1))
  private def form = formProvider()

  private val validData = Map(
    "dateOfBirth.day" -> testDateOfBirth.getDayOfMonth.toString,
    "dateOfBirth.month" -> testDateOfBirth.getMonthValue.toString,
    "dateOfBirth.year" -> testDateOfBirth.getYear.toString,
    "dateOfDeath.day" -> testDateOfDeath.getDayOfMonth.toString,
    "dateOfDeath.month" -> testDateOfDeath.getMonthValue.toString,
    "dateOfDeath.year" -> testDateOfDeath.getYear.toString
  )

  ".bind" - {

    "must reject a death date before the configured minimum and include that date in the error" in {
      val provider = providerWithMinimum(LocalDate.of(2027, 4, 6))
      val result = provider.validate(provider().bind(validData))

      result.errors must contain(
        FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.minimum", Seq("6 April 2027"))
      )
    }

    "must accept a death date on the configured minimum" in {
      val provider = providerWithMinimum(testDateOfDeath)
      val result = provider.validate(provider().bind(validData))

      result.errors mustBe empty
    }

    "must accept a death date after the configured minimum" in {
      val provider = providerWithMinimum(testDateOfDeath.minusDays(1))
      val result = provider.validate(provider().bind(validData))

      result.errors mustBe empty
    }

    "must reject the day immediately before the configured minimum" in {
      val provider = providerWithMinimum(testDateOfDeath.plusDays(1))
      val result = provider.validate(provider().bind(validData))

      result.errors.map(_.message) must contain("birthDeathDates.dateOfDeath.error.minimum")
    }

    "must still check the minimum death date when the birth date is missing" in {
      val provider = providerWithMinimum(LocalDate.of(2027, 4, 6))
      val data = validData.filterNot { case (key, _) => key.startsWith("dateOfBirth.") }
      val result = provider.validate(provider().bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.required.all"))
      result.errors must contain(
        FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.minimum", Seq("6 April 2027"))
      )
    }

    Seq(
      "missing" -> Map("dateOfDeath.day" -> "", "dateOfDeath.month" -> "", "dateOfDeath.year" -> ""),
      "invalid" -> Map("dateOfDeath.day" -> "31", "dateOfDeath.month" -> "2"),
      "future" -> Map("dateOfDeath.year" -> "2999")
    ).foreach { case (description, invalidDeathDate) =>
      s"preserve existing errors for a $description death date without adding a minimum error" in {
        val provider = providerWithMinimum(LocalDate.of(2027, 4, 6))
        val result = provider.validate(provider().bind(validData ++ invalidDeathDate))

        result.errors must not be empty
        result.errors.map(_.message) must not contain "birthDeathDates.dateOfDeath.error.minimum"
      }
    }

    "must bind birth and death dates" in {
      val result = formProvider.validate(form.bind(validData))

      result.value.value mustEqual BirthDeathDates(testDateOfBirth, testDateOfDeath)
    }

    "must bind a birth and death date when month names are entered in full or short format" in {
      val data = validData ++ Map(
        "dateOfBirth.month" -> "January",
        "dateOfDeath.month" -> "Jan"
      )

      val result = formProvider.validate(form.bind(data))

      result.value.value mustEqual BirthDeathDates(testDateOfBirth, testDateOfDeath)
    }

    "must bind birth and death dates when whitespace is entered in the date fields" in {
      val data = Map(
        "dateOfBirth.day" -> " 1 ",
        "dateOfBirth.month" -> " Jan uary ",
        "dateOfBirth.year" -> " 1 950 ",
        "dateOfDeath.day" -> " 1 ",
        "dateOfDeath.month" -> " J an ",
        "dateOfDeath.year" -> " 2 020 "
      )

      val result = formProvider.validate(form.bind(data))

      result.value.value mustEqual BirthDeathDates(testDateOfBirth, testDateOfDeath)
    }

    "must show separate errors when the birth and death date fields are empty" in {
      val data = Map(
        "dateOfBirth.day" -> "",
        "dateOfBirth.month" -> "",
        "dateOfBirth.year" -> "",
        "dateOfDeath.day" -> "",
        "dateOfDeath.month" -> "",
        "dateOfDeath.year" -> ""
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.required.all"))
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.required.all"))
    }

    "must error when the birth date is not in the past" in {
      val today = LocalDate.now()
      val data = validData ++ Map(
        "dateOfBirth.day" -> today.getDayOfMonth.toString,
        "dateOfBirth.month" -> today.getMonthValue.toString,
        "dateOfBirth.year" -> today.getYear.toString
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.past"))
    }

    "must error when the death date is not in the past" in {
      val data = validData ++ Map(
        "dateOfDeath.year" -> "2999"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.past"))
    }

    "must error when both dates are not in the past" in {
      val today = LocalDate.now()
      val data = validData ++ Map(
        "dateOfBirth.day" -> today.getDayOfMonth.toString,
        "dateOfBirth.month" -> today.getMonthValue.toString,
        "dateOfBirth.year" -> today.getYear.toString,
        "dateOfDeath.day" -> today.getDayOfMonth.toString,
        "dateOfDeath.month" -> today.getMonthValue.toString,
        "dateOfDeath.year" -> today.getYear.toString
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.past"))
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.past"))
    }

    "must error when the birth date is not before the death date" in {
      val data = validData ++ Map(
        "dateOfBirth.year" -> "2020"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.error.birthBeforeDeath"))
    }

    "must error when the birth date is not after 01/01/1900" in {
      val data = validData ++ Map(
        "dateOfBirth.day" -> "1",
        "dateOfBirth.month" -> "1",
        "dateOfBirth.year" -> "1900"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.error.birthAfter1900"))
    }

    "must error when the birth date is not after 01/01/1900 and the death date has errors" in {
      val data = validData ++ Map(
        "dateOfBirth.day" -> "1",
        "dateOfBirth.month" -> "1",
        "dateOfBirth.year" -> "1900",
        "dateOfDeath.day" -> "",
        "dateOfDeath.month" -> "",
        "dateOfDeath.year" -> ""
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.error.birthAfter1900"))
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.required.all"))
    }

    "must error when birth day is out of range (less than 1)" in {
      val data = validData ++ Map(
        "dateOfBirth.day" -> "0"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid.day"))
      result.errors must not contain FormError("dateOfBirth.day", "birthDeathDates.dateOfBirth.error.invalid.day")
    }

    "must error when birth day is out of range (greater than 31)" in {
      val data = validData ++ Map(
        "dateOfBirth.day" -> "32"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid.day"))
      result.errors must not contain FormError("dateOfBirth.day", "birthDeathDates.dateOfBirth.error.invalid.day")
    }

    "must error when birth month is out of range (greater than 12)" in {
      val data = validData ++ Map(
        "dateOfBirth.month" -> "13"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid.month"))
      result.errors must not contain FormError("dateOfBirth.month", "birthDeathDates.dateOfBirth.error.invalid.month")
    }

    "must error when birth year is out of range (less than 1)" in {
      val data = validData ++ Map(
        "dateOfBirth.year" -> "0"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must not contain FormError("dateOfBirth.year", "birthDeathDates.dateOfBirth.error.invalid.year")
    }

    "must error when death day is out of range" in {
      val data = validData ++ Map(
        "dateOfDeath.day" -> "32"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.invalid.day"))
      result.errors must not contain FormError("dateOfDeath.day", "birthDeathDates.dateOfDeath.error.invalid.day")
    }

    "must error when death month is out of range" in {
      val data = validData ++ Map(
        "dateOfDeath.month" -> "13"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.invalid.month"))
      result.errors must not contain FormError("dateOfDeath.month", "birthDeathDates.dateOfDeath.error.invalid.month")
    }

    "must error when birth year is before 1900 when date cannot be parsed" in {
      val data = Map(
        "dateOfBirth.day" -> "33",
        "dateOfBirth.month" -> "13",
        "dateOfBirth.year" -> "1800",
        "dateOfDeath.day" -> "1",
        "dateOfDeath.month" -> "1",
        "dateOfDeath.year" -> "2020"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid"))
      result.errors must not contain FormError("dateOfBirth", "birthDeathDates.error.birthAfter1900")
    }

    "must show multiple errors for different fields" in {
      val data = Map(
        "dateOfBirth.day" -> "1",
        "dateOfBirth.month" -> "1",
        "dateOfBirth.year" -> "1800",
        "dateOfDeath.day" -> "1",
        "dateOfDeath.month" -> "1",
        "dateOfDeath.year" -> "2999"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.error.birthAfter1900"))
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.past"))
    }

    "must show only parent error when multiple sub-fields are invalid for same date" in {
      val data = Map(
        "dateOfBirth.day" -> "33",
        "dateOfBirth.month" -> "13",
        "dateOfBirth.year" -> "1950",
        "dateOfDeath.day" -> "1",
        "dateOfDeath.month" -> "1",
        "dateOfDeath.year" -> "2020"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must not contain FormError("dateOfBirth.day", "birthDeathDates.dateOfBirth.error.invalid.day")
      result.errors must not contain FormError("dateOfBirth.month", "birthDeathDates.dateOfBirth.error.invalid.month")
      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid"))
    }

    "must show only sub-field error when only one sub-field is invalid" in {
      val data = Map(
        "dateOfBirth.day" -> "33",
        "dateOfBirth.month" -> "1",
        "dateOfBirth.year" -> "1950",
        "dateOfDeath.day" -> "1",
        "dateOfDeath.month" -> "1",
        "dateOfDeath.year" -> "2020"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid.day"))
      result.errors must not contain FormError("dateOfBirth.day", "birthDeathDates.dateOfBirth.error.invalid.day")
    }

    "must show only parent error when multiple sub-fields are invalid for death date" in {
      val data = Map(
        "dateOfBirth.day" -> "1",
        "dateOfBirth.month" -> "1",
        "dateOfBirth.year" -> "1950",
        "dateOfDeath.day" -> "32",
        "dateOfDeath.month" -> "13",
        "dateOfDeath.year" -> "2020"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must not contain FormError("dateOfDeath.day", "birthDeathDates.dateOfDeath.error.invalid.day")
      result.errors must not contain FormError("dateOfDeath.month", "birthDeathDates.dateOfDeath.error.invalid.month")
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.invalid"))
    }

    "must show both birth and death errors when both have issues" in {
      val data = Map(
        "dateOfBirth.day" -> "33",
        "dateOfBirth.month" -> "13",
        "dateOfBirth.year" -> "1701",
        "dateOfDeath.day" -> "32",
        "dateOfDeath.month" -> "13",
        "dateOfDeath.year" -> "2028"
      )

      val result = formProvider.validate(form.bind(data))

      result.errors must contain(FormError("dateOfBirth", "birthDeathDates.dateOfBirth.error.invalid"))
      result.errors must contain(FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.invalid"))
      result.errors must not contain FormError("dateOfDeath", "birthDeathDates.dateOfDeath.error.past")
      result.errors must not contain FormError("dateOfBirth", "birthDeathDates.error.birthAfter1900")
    }
  }
}
