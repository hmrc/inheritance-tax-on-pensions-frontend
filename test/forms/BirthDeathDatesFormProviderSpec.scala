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
import models.BirthDeathDates
import play.api.i18n.Messages
import play.api.data.FormError

import java.time.LocalDate

class BirthDeathDatesFormProviderSpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()
  private val formProvider = new BirthDeathDatesFormProvider()
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
  }
}
