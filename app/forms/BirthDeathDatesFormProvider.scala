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

import forms.mappings.Mappings
import play.api.data.Forms.mapping
import models.BirthDeathDates
import play.api.i18n.Messages
import play.api.data.Form

import scala.util.Try

import java.time.{LocalDate, Month, ZoneOffset}
import javax.inject.Inject

class BirthDeathDatesFormProvider @Inject() extends Mappings {

  private val dateOfBirthKey = "dateOfBirth"
  private val dateOfDeathKey = "dateOfDeath"
  private val earliestBirthDate = LocalDate.of(1900, 1, 1)

  def apply()(implicit messages: Messages): Form[BirthDeathDates] =
    Form(
      mapping(
        dateOfBirthKey -> localDate(
          invalidKey = "birthDeathDates.dateOfBirth.error.invalid",
          allRequiredKey = "birthDeathDates.dateOfBirth.error.required.all",
          twoRequiredKey = "birthDeathDates.dateOfBirth.error.required.two",
          requiredKey = "birthDeathDates.dateOfBirth.error.required"
        ),
        dateOfDeathKey -> localDate(
          invalidKey = "birthDeathDates.dateOfDeath.error.invalid",
          allRequiredKey = "birthDeathDates.dateOfDeath.error.required.all",
          twoRequiredKey = "birthDeathDates.dateOfDeath.error.required.two",
          requiredKey = "birthDeathDates.dateOfDeath.error.required"
        )
      )(BirthDeathDates.apply)(date => Some((date.dateOfBirth, date.dateOfDeath)))
    )

  def validate(form: Form[BirthDeathDates]): Form[BirthDeathDates] = {
    val formattedForm = form.copy(data = form.data.view.mapValues(_.replaceAll("\\s+", "")).toMap)

    form.value match {
      case None =>
        formattedForm.withEarliestBirthDateError
      case Some(dates) =>
        val today = LocalDate.now(ZoneOffset.UTC)

        if (!dates.dateOfBirth.isAfter(earliestBirthDate)) {
          formattedForm.withError(dateOfBirthKey, "birthDeathDates.error.birthAfter1900")
        } else if (!dates.dateOfBirth.isBefore(today) || !dates.dateOfDeath.isBefore(today)) {
          formattedForm.withPastDateErrors(dates, today)
        } else if (!dates.dateOfBirth.isBefore(dates.dateOfDeath)) {
          formattedForm.withError(dateOfBirthKey, "birthDeathDates.error.birthBeforeDeath")
        } else {
          formattedForm
        }
    }
  }

  extension (form: Form[BirthDeathDates])
    private def withEarliestBirthDateError: Form[BirthDeathDates] =
      parsedDateOfBirth(form.data)
        .filterNot(_.isAfter(earliestBirthDate))
        .fold(form) { _ =>
          if (form.errors.exists(_.key == dateOfBirthKey)) {
            form
          } else {
            form.withError(dateOfBirthKey, "birthDeathDates.error.birthAfter1900")
          }
        }

    private def withPastDateErrors(dates: BirthDeathDates, today: LocalDate): Form[BirthDeathDates] = {
      val formWithBirthDateError =
        if (!dates.dateOfBirth.isBefore(today)) {
          form.withError(dateOfBirthKey, "birthDeathDates.dateOfBirth.error.past")
        } else {
          form
        }

      if (!dates.dateOfDeath.isBefore(today)) {
        formWithBirthDateError.withError(dateOfDeathKey, "birthDeathDates.dateOfDeath.error.past")
      } else {
        formWithBirthDateError
      }
    }

  private def parsedDateOfBirth(data: Map[String, String]): Option[LocalDate] =
    for {
      day <- formattedInt(data, s"$dateOfBirthKey.day")
      month <- formattedMonth(data, s"$dateOfBirthKey.month")
      year <- formattedInt(data, s"$dateOfBirthKey.year")
      date <- Try(LocalDate.of(year, month, day)).toOption
    } yield date

  private def formattedInt(data: Map[String, String], key: String): Option[Int] =
    data
      .get(key)
      .map(_.replaceAll("\\s+", ""))
      .filter(_.nonEmpty)
      .flatMap(value => Try(value.toInt).toOption)

  private def formattedMonth(data: Map[String, String], key: String): Option[Int] =
    data
      .get(key)
      .map(_.replaceAll("\\s+", "").toUpperCase)
      .filter(_.nonEmpty)
      .flatMap { value =>
        Month.values.toList
          .find(month =>
            month.getValue.toString == value.replaceAll("^0+", "") ||
              month.toString == value ||
              month.toString.take(3) == value
          )
          .map(_.getValue)
      }
}
