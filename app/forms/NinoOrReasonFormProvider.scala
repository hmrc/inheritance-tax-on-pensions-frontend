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
import play.api.data.Forms.{mapping, optional}
import models.NinoOrReason
import play.api.data.Form

import javax.inject.Inject

class NinoOrReasonFormProvider @Inject() extends Mappings {

  private val ninoMapping = nino(
    requiredKey = "ninoOrReason.nino.error.required",
    invalidKey = "ninoOrReason.nino.error.invalid"
  )

  private val reasonForNoNinoMapping = reasonForNoNino(
    requiredKey = "ninoOrReason.reasonForNoNino.error.required",
    invalidKey = "ninoOrReason.reasonForNoNino.error.invalid",
    maxLengthKey = "ninoOrReason.reasonForNoNino.error.length"
  )

  def apply(): Form[NinoOrReasonFormData] =
    Form(
      mapping(
        "value" -> enumerable[NinoOrReason]("ninoOrReason.error.required"),
        "nino" -> optional(text()),
        "reasonForNoNino" -> optional(text())
      )(NinoOrReasonFormData.apply)(data => Some((data.value, data.nino, data.reasonForNoNino)))
    )

  def validate(form: Form[NinoOrReasonFormData]): Form[NinoOrReasonFormData] =
    form.value match {
      case None => form
      case Some(data) =>
        val formattedData = clearUnselectedField(formatData(data))

        formattedData.value match {
          case NinoOrReason.Yes =>
            validateField(
              form.fill(formattedData),
              fieldName = "nino",
              fieldValue = formattedData.nino,
              mapping = ninoMapping
            )

          case NinoOrReason.No =>
            validateField(
              form.fill(formattedData),
              fieldName = "reasonForNoNino",
              fieldValue = formattedData.reasonForNoNino,
              mapping = reasonForNoNinoMapping
            )
        }
    }

  private def clearUnselectedField(data: NinoOrReasonFormData): NinoOrReasonFormData =
    data.value match {
      case NinoOrReason.Yes => data.copy(reasonForNoNino = None)
      case NinoOrReason.No => data.copy(nino = None)
    }

  private def formatData(data: NinoOrReasonFormData): NinoOrReasonFormData =
    data.copy(
      nino = data.nino
        .map(_.replaceAll("\\s+", "").toUpperCase)
        .filter(_.nonEmpty),
      reasonForNoNino = data.reasonForNoNino
        .map(_.trim)
        .filter(_.nonEmpty)
    )

  private def validateField(
    form: Form[NinoOrReasonFormData],
    fieldName: String,
    fieldValue: Option[String],
    mapping: play.api.data.Mapping[String]
  ): Form[NinoOrReasonFormData] =
    fieldValue match {
      case None =>
        form.withError(fieldName, s"ninoOrReason.$fieldName.error.required")
      case Some(value) =>
        mapping.withPrefix(fieldName).bind(Map(fieldName -> value)) match {
          case Left(errors) => form.withError(errors.head)
          case Right(normalisedValue) =>
            fieldName match {
              case "nino" => form.fill(form.get.copy(nino = Some(normalisedValue)))
              case "reasonForNoNino" => form.fill(form.get.copy(reasonForNoNino = Some(normalisedValue)))
              case _ => form
            }
        }
    }
}
