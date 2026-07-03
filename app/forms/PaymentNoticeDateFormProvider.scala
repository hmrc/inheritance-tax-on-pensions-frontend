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
import play.api.data.Forms.single
import play.api.i18n.Messages
import play.api.data.Form

import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PaymentNoticeDateFormProvider @Inject() extends Mappings {

  private val fieldKey = "dateThePensionSchemeReceivedNoticeToPay"
  private val errorDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      single(
        fieldKey -> localDate(
          invalidKey = "paymentNoticeDate.error.invalid",
          allRequiredKey = "paymentNoticeDate.error.required.all",
          twoRequiredKey = "paymentNoticeDate.error.required.two",
          requiredKey = "paymentNoticeDate.error.required"
        )
      )
    )

  def validate(form: Form[LocalDate], dateOfDeath: Option[LocalDate] = None): Form[LocalDate] = {
    val formattedForm = form.copy(data = form.data.view.mapValues(_.replaceAll("\\s+", "")).toMap)

    (formattedForm.value, dateOfDeath) match {
      case (Some(date), Some(deathDate)) if date.isBefore(deathDate) || date.isEqual(deathDate) =>
        formattedForm.withError(
          fieldKey,
          "paymentNoticeDate.error.afterDateOfDeath",
          deathDate.format(errorDateFormatter)
        )
      case (Some(date), _) if !date.isBefore(LocalDate.now(ZoneOffset.UTC)) =>
        formattedForm.withError(fieldKey, "paymentNoticeDate.error.past")
      case _ =>
        formattedForm
    }
  }
}
