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

import services.{AddressLookupFrontendService, UserAnswersService}
import utils.BeneficiaryNameHelper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import controllers.IhtpBaseController
import models.SchemeId.Srn
import controllers.actions._
import models.beneficiary.BeneficiaryType
import models._
import pages.beneficiary._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryAddressController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  addressLookupFrontendService: AddressLookupFrontendService,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def start(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      beneficiaryDetails(request.userAnswers, index) match {
        case Some((role, name)) =>
          addressLookupFrontendService
            .initBeneficiaryJourney(srn, index, mode, name, role)
            .map(Redirect(_))
        case None => Future.successful(logAndJourneyRecovery("Beneficiary details are incomplete for address lookup"))
      }
    }

  def continue(srn: Srn, index: Int, mode: Mode, id: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      if (id.trim.isEmpty || beneficiaryDetails(request.userAnswers, index).isEmpty) {
        Future.successful(logAndJourneyRecovery("Missing address lookup id or incomplete beneficiary details"))
      } else {
        addressLookupFrontendService.getAddress(id).flatMap { addressData =>
          if (!PrAddress.hasValidFirstAddressLine(addressData)) {
            Future.successful(logAndJourneyRecovery("No address lines returned for beneficiary"))
          } else {
            for {
              answers <- Future.fromTry(
                request.userAnswers.set(BeneficiaryAddressPage(index), PrAddress.fromAlfAddressData(addressData))
              )
              saved <- userAnswersService.set(answers)(using hc, request.request)
            } yield saved.fold(
              _ => logAndJourneyRecovery("Unable to save beneficiary address"),
              updated =>
                Redirect(mode match {
                  case CheckMode if updated.get(BeneficiaryIhtPayablePage(index)).isDefined =>
                    controllers.routes.CheckYourAnswersController.onPageLoad(srn)
                  case _ => routes.BeneficiaryIhtPayableController.onPageLoad(srn, index, mode)
                })
            )
          }
        }
      }
    }

  private def beneficiaryDetails(answers: UserAnswers, index: Int): Option[(JourneyRole, String)] =
    if (
      index < 0 || index >= 30 ||
      !answers.get(AreBeneficiariesKnownPage).getOrElse(answers.get(DidPrSubmitPage).contains(false))
    ) None
    else
      for {
        name <- BeneficiaryNameHelper.fromUserAnswers(answers, index)
        role <- answers.get(BeneficiaryTypePage(index)) match {
          case Some(BeneficiaryType.Individual) if answers.get(BeneficiaryHasNinoPage(index)).exists {
                case true => answers.get(BeneficiaryNinoPage(index)).isDefined
                case false => answers.get(BeneficiaryNoNinoReasonPage(index)).isDefined
              } =>
            Some(JourneyRole.BeneficiaryIndividual)
          case Some(BeneficiaryType.Trust)
              if answers.get(BeneficiaryNamePage(index, JourneyRole.BeneficiaryTrust)).isDefined =>
            Some(JourneyRole.BeneficiaryTrust)
          case _ => None
        }
      } yield (role, name)
}
