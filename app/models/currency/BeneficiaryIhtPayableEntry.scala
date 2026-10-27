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

import utils.BeneficiaryNameHelper
import play.api.mvc.Call
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import models.SchemeId.Srn
import models.beneficiary.BeneficiaryType
import controllers.beneficiary.routes
import models._
import pages.beneficiary._

case class BeneficiaryIhtPayableEntry(index: Int) extends CurrencyEntry {
  override val page = BeneficiaryIhtPayablePage(index)
  override val messagePrefix = "beneficiaryIhtPayable"
  override def isAllowed(answers: UserAnswers): Boolean =
    index >= 0 && index < 30 &&
      answers.get(AreBeneficiariesKnownPage).getOrElse(answers.get(DidPrSubmitPage).contains(false)) &&
      BeneficiaryNameHelper.fromUserAnswers(answers, index).isDefined &&
      answers.get(BeneficiaryAddressPage(index)).isDefined &&
      (answers.get(BeneficiaryTypePage(index)) match {
        case Some(BeneficiaryType.Individual) =>
          answers.get(BeneficiaryHasNinoPage(index)).exists {
            case true => answers.get(BeneficiaryNinoPage(index)).isDefined
            case false => answers.get(BeneficiaryNoNinoReasonPage(index)).isDefined
          }
        case Some(BeneficiaryType.Trust) =>
          answers.get(BeneficiaryNamePage(index, JourneyRole.BeneficiaryTrust)).isDefined
        case _ => false
      })
  override def messageArgs(answers: UserAnswers): Seq[String] =
    BeneficiaryNameHelper.fromUserAnswers(answers, index).toSeq
  override def submitAction(srn: Srn, mode: Mode): Call =
    routes.BeneficiaryIhtPayableController.onSubmit(srn, index, mode)
  override def nextPage(srn: Srn, mode: Mode, answers: UserAnswers): Call =
    mode match {
      case NormalMode => routes.BeneficiaryInterestPayableController.onPageLoad(srn, index, NormalMode)
      case CheckMode if answers.get(BeneficiaryInterestPayablePage(index)).isEmpty =>
        routes.BeneficiaryInterestPayableController.onPageLoad(srn, index, CheckMode)
      case CheckMode => controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
