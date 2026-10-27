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

import services.UserAnswersService
import utils.BeneficiaryNameHelper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryNinoView
import controllers.actions._
import forms.beneficiary.BeneficiaryNinoFormProvider
import uk.gov.hmrc.domain.Nino
import models._
import pages.beneficiary.{BeneficiaryAddressPage, BeneficiaryIhtPayablePage, BeneficiaryNinoPage}
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryNinoController @Inject() (
  override val messagesApi: MessagesApi,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryNinoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  beneficiaryAccess: BeneficiaryAccessAction,
  view: BeneficiaryNinoView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess) { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index)(
          logAndJourneyRecovery("Beneficiary name is missing, cannot load the page")
        ) { beneficiaryName =>
          val preparedForm = request.userAnswers.get(BeneficiaryNinoPage(index)) match {
            case None => form
            case Some(value) => form.fill(Nino(value))
          }
          Ok(view(preparedForm, srn, index, mode, beneficiaryName))
        }
      }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess)
      .async { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index)(
          Future.successful(
            logAndJourneyRecovery("Beneficiary name is missing, cannot submit the page")
          )
        ) { beneficiaryName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, index, mode, beneficiaryName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(BeneficiaryNinoPage(index), value.value))
                  _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                } yield Redirect(nextPage(srn, index, mode, updatedAnswers))
            )
        }
      }

  private def nextPage(srn: Srn, index: Int, mode: Mode, answers: UserAnswers) =
    mode match {
      case NormalMode => routes.BeneficiaryAddressController.start(srn, index, NormalMode)
      case CheckMode if answers.get(BeneficiaryAddressPage(index)).isEmpty =>
        routes.BeneficiaryAddressController.start(srn, index, CheckMode)
      case CheckMode if answers.get(BeneficiaryIhtPayablePage(index)).isEmpty =>
        routes.BeneficiaryIhtPayableController.onPageLoad(srn, index, CheckMode)
      case CheckMode => controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
