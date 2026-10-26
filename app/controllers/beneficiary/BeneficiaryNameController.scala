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
import play.api.mvc._
import controllers.{routes, IhtpBaseController}
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryNameView
import controllers.actions._
import forms.beneficiary.BeneficiaryNameFormProvider
import play.api.libs.json._
import models._
import pages.beneficiary._
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.Inject

class BeneficiaryNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  beneficiaryAccess: BeneficiaryAccessAction,
  formProvider: BeneficiaryNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BeneficiaryNameView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode, index: Int, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess) { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            logAndJourneyRecovery("unknown journeyRole, cannot load the page")

          case _ =>
            val form = formProvider(journeyRole)
            val preparedForm = request.userAnswers.get(BeneficiaryNamePage(index, journeyRole)) match {
              case None => form
              case Some(individualName) => form.fill(individualName)
            }

            Ok(view(preparedForm, srn, index, mode, journeyRole, trustName(request.userAnswers, index, journeyRole)))
        }
      }

  def onSubmit(srn: Srn, mode: Mode, index: Int, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess)
      .async { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            Future.successful(logAndJourneyRecovery("unknown journeyRole, cannot submit the page"))

          case _ =>
            formProvider(journeyRole)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        srn,
                        index,
                        mode,
                        journeyRole,
                        trustName(request.userAnswers, index, journeyRole)
                      )
                    )
                  ),
                individualName =>
                  for {
                    updatedAnswers <- Future
                      .fromTry(addBeneficiaryName(request.userAnswers, index, journeyRole, individualName))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, index, mode, journeyRole, updatedAnswers))
              )
        }
      }

  private[controllers] def addBeneficiaryName(
    userAnswers: UserAnswers,
    index: Int,
    journeyRole: JourneyRole,
    individualName: IndividualName
  ): Try[UserAnswers] = Try {
    val beneficiaries = (userAnswers.data \ "beneficiaries").as[JsArray]
    val beneficiary = beneficiaries.value(index).as[JsObject]
    val details = (beneficiary \ "beneficiaryDetails").asOpt[JsObject].getOrElse(Json.obj())
    val beneficiaryType = journeyRole.name
    val existing = details.value.get(beneficiaryType).collect { case obj: JsObject => obj }.getOrElse(Json.obj())

    val updatedBeneficiary = beneficiary ++ Json.obj(
      "beneficiaryDetails" -> (details ++ Json.obj(
        beneficiaryType -> existing.deepMerge(Json.toJson(individualName).as[JsObject])
      ))
    )

    userAnswers.copy(
      data = userAnswers.data ++ Json.obj(
        "beneficiaries" -> JsArray(beneficiaries.value.updated(index, updatedBeneficiary))
      )
    )
  }

  private def trustName(userAnswers: UserAnswers, index: Int, journeyRole: JourneyRole): Option[String] =
    Option.when(journeyRole == JourneyRole.BeneficiaryTrust)(userAnswers.get(BeneficiaryTrustNamePage(index))).flatten

  private[controllers] def nextPage(
    srn: Srn,
    index: Int,
    mode: Mode,
    journeyRole: JourneyRole,
    userAnswers: UserAnswers
  ): Call =
    journeyRole match {
      case JourneyRole.BeneficiaryIndividual =>
        mode match {
          case NormalMode =>
            controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, index, NormalMode)
          case CheckMode if userAnswers.get(BeneficiaryHasNinoPage(index)).isEmpty =>
            controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, index, CheckMode)
          case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
        }
      case JourneyRole.BeneficiaryTrust =>
        mode match {
          case NormalMode =>
            controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, index, NormalMode)
          case CheckMode if userAnswers.get(BeneficiaryAddressPage(index)).isEmpty =>
            controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, index, CheckMode)
          case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
        }
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
