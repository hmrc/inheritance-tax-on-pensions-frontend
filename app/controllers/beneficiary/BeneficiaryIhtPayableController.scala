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

package controllers.beneficiary

import play.api.mvc.{Action, AnyContent}
import controllers.CurrencyEntryController
import models.SchemeId.Srn
import models.currency.BeneficiaryIhtPayableEntry
import models.Mode

import javax.inject.Inject

class BeneficiaryIhtPayableController @Inject() (currencyEntry: CurrencyEntryController) {
  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    currencyEntry.onPageLoad(srn, mode, BeneficiaryIhtPayableEntry(index))

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    currencyEntry.onSubmit(srn, mode, BeneficiaryIhtPayableEntry(index))
}
