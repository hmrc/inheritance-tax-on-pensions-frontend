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

package utils

import play.api.mvc.Call
import pages.{AreBeneficiariesKnownPage, IhtPayablePage, QuestionPage}
import controllers.routes
import models.SchemeId.Srn
import models.{CurrencyEntry, Mode, UserAnswers}

case class TestCurrencyEntry(
  override val page: QuestionPage[BigDecimal] = IhtPayablePage,
  override val messagePrefix: String = "ihtPayable",
  override val minimum: BigDecimal = BigDecimal(0),
  override val maximum: BigDecimal = BigDecimal("999999999.99"),
  maximumError: Option[String] = None,
  messageArguments: Seq[String] = Seq.empty
) extends CurrencyEntry {
  override def maximumErrorKey: String = maximumError.getOrElse(super.maximumErrorKey)
  override def messageArgs(answers: UserAnswers): Seq[String] = messageArguments
  override def isAllowed(answers: UserAnswers): Boolean = answers.get(AreBeneficiariesKnownPage).contains(false)
  override def submitAction(srn: Srn, mode: Mode): Call = routes.IhtPayableController.onSubmit(srn, mode)
  override def nextPage(srn: Srn, mode: Mode, answers: UserAnswers): Call =
    routes.CheckYourAnswersController.onPageLoad(srn)
}
