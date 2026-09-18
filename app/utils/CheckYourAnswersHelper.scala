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

import models.JourneyRole.{BeneficiaryIndividual, PrIndividual, PrOrganisation}
import play.api.mvc.Call
import pages._
import controllers.routes
import models.SchemeId.Srn
import models.beneficiary.BeneficiaryType
import models.{NormalMode, PrType, UserAnswers}

object CheckYourAnswersHelper {

  private case class ContinuationPage(
    isUnanswered: UserAnswers => Boolean,
    call: Call
  )

  def findPageToContinue(userAnswers: UserAnswers, srn: Srn): Option[Call] = {
    val allPages = getDeceasedPages(srn) :++ getPrPages(srn) :++ getBeneficiariesPages(userAnswers, srn)
    val found = allPages
      .find(_.isUnanswered(userAnswers))
      .map(_.call)
    found
  }

  private def getDeceasedPages(srn: Srn) =
    Seq(
      ContinuationPage(
        answers => answers.get(HasNinoPage).isEmpty,
        routes.HasNinoController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(HasNinoPage).get && answers.get(NinoPage).isEmpty,
        routes.NinoController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => !answers.get(HasNinoPage).get && answers.get(NoNinoReasonPage).isEmpty,
        routes.NoNinoReasonController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(BirthDeathDatesPage).isEmpty,
        routes.BirthDeathDatesController.onPageLoad(srn, NormalMode)
      )
    )

  private def getPrPages(srn: Srn) =
    Seq(
      ContinuationPage(
        answers => answers.get(PrTypePage).isEmpty,
        routes.PrTypeController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Individual) && answers.get(IndividualNamePage(PrIndividual)).isEmpty,
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrIndividual)
      ),
      ContinuationPage(
        answers => answers.get(PrTypePage).contains(PrType.Individual) && answers.get(PrIndividualAddressPage).isEmpty,
        routes.AddressLookupStartController.start(srn, NormalMode, PrIndividual)
      ),
      ContinuationPage(
        answers => answers.get(PrTypePage).contains(PrType.Organisation) && answers.get(OrganisationNamePage).isEmpty,
        routes.OrganisationNameController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Organisation) && answers
            .get(IndividualNamePage(PrOrganisation))
            .isEmpty,
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrOrganisation)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Organisation) && answers.get(PrOrganisationAddressPage).isEmpty,
        routes.AddressLookupStartController.start(srn, NormalMode, PrOrganisation)
      ),
      ContinuationPage(
        answers => answers.get(DidPrSubmitPage).isEmpty,
        routes.DidPrSubmitController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(PaymentNoticeDatePage).isEmpty,
        routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(DidPrSubmitPage).get && answers.get(AreBeneficiariesKnownPage).isEmpty,
        routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode)
      )
    )

  private def getBeneficiaryPages(srn: Srn, i: Int) =
    Seq(
      ContinuationPage(
        answers =>
          answers.get(beneficiary.BeneficiaryTypePage(i)).contains(BeneficiaryType.Individual) &&
            answers.get(beneficiary.BeneficiaryNamePage(i, BeneficiaryIndividual)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryNameController.onPageLoad(srn, NormalMode, i)
      ),
      ContinuationPage(
        answers =>
          answers.get(beneficiary.BeneficiaryTypePage(i)).contains(BeneficiaryType.Trust) &&
            answers.get(pages.beneficiary.BeneficiaryTrustNamePage(i)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryTrustNameController.onPageLoad(srn, i, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(pages.beneficiary.BeneficiaryHasNinoPage(i)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, i, NormalMode)
      )
    )

  private def getBeneficiariesPages(answers: UserAnswers, srn: Srn) = {
    val numberOfBeneficiaries =
      answers.get(pages.beneficiary.BeneficiariesPage()).map(_.beneficiaries.size).getOrElse(0)

    (0 until numberOfBeneficiaries).flatMap(i => getBeneficiaryPages(srn, i))
  }
}
