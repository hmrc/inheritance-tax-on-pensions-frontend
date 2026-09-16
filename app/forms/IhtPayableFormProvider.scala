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
import play.api.data.Form

import javax.inject.Inject

class IhtPayableFormProvider @Inject() extends Mappings {
  private val MinIhtPayable: BigDecimal = BigDecimal(0)
  private val MaxIhtPayable: BigDecimal = BigDecimal("999999999.99")

  def apply(): Form[BigDecimal] = Form(
    "value" -> currency(
      requiredKey = "ihtPayable.error.required",
      invalidNumeric = "ihtPayable.error.invalid",
      nonNumericKey = "ihtPayable.error.invalid"
    ).verifying(
      minimumCurrency(MinIhtPayable, "ihtPayable.error.invalid"),
      maximumCurrency(MaxIhtPayable, "ihtPayable.error.invalid")
    )
  )
}
