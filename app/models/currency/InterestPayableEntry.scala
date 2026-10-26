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

package models.currency

import play.api.mvc.Call
import pages.{AreBeneficiariesKnownPage, InterestPayablePage}
import controllers.routes
import models.SchemeId.Srn
import models.{CurrencyEntry, Mode, UserAnswers}

case object InterestPayableEntry extends CurrencyEntry {
  override val page = InterestPayablePage
  override val messagePrefix = "interestPayable"
  override val maximumErrorKey = "interestPayable.error.invalid"
  override def isAllowed(answers: UserAnswers): Boolean = answers.get(AreBeneficiariesKnownPage).contains(false)
  override def submitAction(srn: Srn, mode: Mode): Call = routes.InterestPayableController.onSubmit(srn, mode)
  override def nextPage(srn: Srn, mode: Mode, answers: UserAnswers): Call =
    routes.CheckYourAnswersController.onPageLoad(srn)
}
