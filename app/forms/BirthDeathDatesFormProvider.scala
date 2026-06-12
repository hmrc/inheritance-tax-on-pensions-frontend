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
import play.api.data.{Form, FormError}

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
        dateOfBirthKey -> localDateConfig(dateOfBirthKey),
        dateOfDeathKey -> localDateConfig(dateOfDeathKey)
      )(BirthDeathDates.apply)(date => Some((date.dateOfBirth, date.dateOfDeath)))
    )

  private def localDateConfig(key: String)(implicit messages: Messages) =
    localDate(
      invalidKey = s"birthDeathDates.$key.error.invalid",
      allRequiredKey = s"birthDeathDates.$key.error.required.all",
      twoRequiredKey = s"birthDeathDates.$key.error.required.two",
      requiredKey = s"birthDeathDates.$key.error.required"
    )

  def validate(form: Form[BirthDeathDates]): Form[BirthDeathDates] = {
    val formattedForm = form.copy(data = form.data.view.mapValues(_.replaceAll("\\s+", "")).toMap)

    val formWithRangeErrors = List(dateOfBirthKey, dateOfDeathKey).foldLeft(formattedForm) { (form, key) =>
      List("day", "month", "year").foldLeft(form) { (form, field) =>
        validateFieldRange(form, key, field, formattedForm.data)
      }
    }

    val formWithParentErrors = addParentKeyErrors(formWithRangeErrors)

    formWithParentErrors.value match {
      case None =>
        val hasBirthErrors =
          formWithRangeErrors.errors.exists(e => e.key == dateOfBirthKey || e.key.startsWith(s"$dateOfBirthKey."))
        val hasDeathErrors =
          formWithRangeErrors.errors.exists(e => e.key == dateOfDeathKey || e.key.startsWith(s"$dateOfDeathKey."))
        formWithParentErrors
          .withEarliestBirthDateError(hasBirthErrors)
          .withFutureYearErrors(hasBirthErrors, hasDeathErrors)
      case Some(dates) =>
        val today = LocalDate.now(ZoneOffset.UTC)
        val hasBirthErrors =
          formWithRangeErrors.errors.exists(e => e.key == dateOfBirthKey || e.key.startsWith(s"$dateOfBirthKey."))
        val hasDeathErrors =
          formWithRangeErrors.errors.exists(e => e.key == dateOfDeathKey || e.key.startsWith(s"$dateOfDeathKey."))

        val withBirthAfter1900 = if (!dates.dateOfBirth.isAfter(earliestBirthDate) && !hasBirthErrors) {
          formWithParentErrors.withError(dateOfBirthKey, "birthDeathDates.error.birthAfter1900")
        } else {
          formWithParentErrors
        }

        val withBirthPast = if (!dates.dateOfBirth.isBefore(today) && !hasBirthErrors) {
          withBirthAfter1900.withError(dateOfBirthKey, "birthDeathDates.dateOfBirth.error.past")
        } else {
          withBirthAfter1900
        }

        val withDeathPast = if (!dates.dateOfDeath.isBefore(today) && !hasDeathErrors) {
          withBirthPast.withError(dateOfDeathKey, "birthDeathDates.dateOfDeath.error.past")
        } else {
          withBirthPast
        }

        val withBirthBeforeDeath =
          if (
            dates.dateOfBirth.isAfter(earliestBirthDate) &&
            dates.dateOfBirth.isBefore(today) &&
            dates.dateOfDeath.isBefore(today) &&
            !dates.dateOfBirth.isBefore(dates.dateOfDeath) &&
            !hasBirthErrors &&
            !hasDeathErrors
          ) {
            withDeathPast.withError(dateOfBirthKey, "birthDeathDates.error.birthBeforeDeath")
          } else {
            withDeathPast
          }

        withBirthBeforeDeath
    }
  }

  extension (form: Form[BirthDeathDates])
    private def withEarliestBirthDateError(hasBirthErrors: Boolean): Form[BirthDeathDates] =
      parsedDateOfBirth(form.data)
        .filterNot(_.isAfter(earliestBirthDate))
        .fold(form) { _ =>
          if (form.errors.exists(_.key == dateOfBirthKey) || hasBirthErrors) {
            form
          } else {
            form.withError(dateOfBirthKey, "birthDeathDates.error.birthAfter1900")
          }
        }

    private def withFutureYearErrors(
      hasBirthErrors: Boolean,
      hasDeathErrors: Boolean
    ): Form[BirthDeathDates] = {
      val today = LocalDate.now(ZoneOffset.UTC)
      val currentYear = today.getYear

      val birthYearError = parsedYear(form.data, dateOfBirthKey).flatMap { year =>
        if (year > currentYear && !hasBirthErrors) {
          Some((dateOfBirthKey, "birthDeathDates.dateOfBirth.error.past"))
        } else if (
          year < earliestBirthDate.getYear && !form.errors.exists(_.key == dateOfBirthKey) && !hasBirthErrors
        ) {
          Some((dateOfBirthKey, "birthDeathDates.error.birthAfter1900"))
        } else {
          None
        }
      }

      val deathYearError = parsedYear(form.data, dateOfDeathKey).flatMap { year =>
        if (year > currentYear && !hasDeathErrors) {
          Some((dateOfDeathKey, "birthDeathDates.dateOfDeath.error.past"))
        } else {
          None
        }
      }

      List(birthYearError, deathYearError).flatten.foldLeft(form) { (form, error) =>
        form.withError(error._1, error._2)
      }
    }

  private def parsedDateOfBirth(data: Map[String, String]): Option[LocalDate] =
    for {
      day <- formattedInt(data, s"$dateOfBirthKey.day")
      month <- formattedMonth(data, s"$dateOfBirthKey.month")
      year <- formattedInt(data, s"$dateOfBirthKey.year")
      date <- Try(LocalDate.of(year, month, day)).toOption
    } yield date

  private def parsedYear(data: Map[String, String], key: String): Option[Int] =
    formattedInt(data, s"$key.year")

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

  private def validateFieldRange(
    form: Form[BirthDeathDates],
    key: String,
    field: String,
    data: Map[String, String]
  ): Form[BirthDeathDates] = {
    val fieldKey = s"$key.$field"
    formattedInt(data, fieldKey).fold(form) { value =>
      val isValid = field match {
        case "day" => value >= 1 && value <= 31
        case "month" => value >= 1 && value <= 12
        case "year" => value >= 1
        case _ => true
      }

      if (isValid) {
        form
      } else if (form.errors.exists(_.key == fieldKey)) {
        form
      } else {
        form.withError(fieldKey, s"birthDeathDates.$key.error.invalid.$field")
      }
    }
  }

  private def addParentKeyErrors(form: Form[BirthDeathDates]): Form[BirthDeathDates] = {
    val originalErrors = form.errors
    List(dateOfBirthKey, dateOfDeathKey).foldLeft(form) { (form, key) =>
      processDateFieldErrors(form, key, originalErrors)
    }
  }

  private def processDateFieldErrors(
    form: Form[BirthDeathDates],
    dateKey: String,
    originalErrors: Seq[play.api.data.FormError]
  ): Form[BirthDeathDates] = {
    val subFieldKeys = Set(s"$dateKey.day", s"$dateKey.month", s"$dateKey.year")
    val subFieldErrors = originalErrors.filter(e => subFieldKeys.contains(e.key))

    if (subFieldErrors.size >= 2) {
      form.copy(errors = form.errors.filterNot(e => subFieldKeys.contains(e.key)))
    } else if (subFieldErrors.nonEmpty) {
      val subFieldError = subFieldErrors.head
      form.copy(errors = form.errors.flatMap { error =>
        if (error.key == dateKey) {
          Some(FormError(dateKey, subFieldError.message, subFieldError.args))
        } else if (subFieldKeys.contains(error.key)) {
          None
        } else {
          Some(error)
        }
      })
    } else {
      form
    }
  }
}
