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

package controllers

import services.{AddressLookupFrontendService, UserAnswersService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.LprIndividualAddressPage
import models.SchemeId.Srn
import controllers.actions._
import play.api.libs.json.{JsObject, JsSuccess, Json}
import models._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AddressLookupContinueController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  addressLookupFrontendService: AddressLookupFrontendService,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def continue(srn: Srn, mode: Mode, id: String): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        if (id.trim.isEmpty) {
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          for {
            addressData <- addressLookupFrontendService.getAddress(id)
            result <-
              if (LprAddress.hasValidFirstAddressLine(addressData)) {
                val updatedAnswers =
                  addLprIndividualAddress(request.userAnswers, LprAddress.fromAlfAddressData(addressData))

                userAnswersService
                  .set(updatedAnswers)(using hc, request.request)
                  .map(_ => Redirect(nextPage(srn, mode)))
              } else {
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              }
          } yield result
        }
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case NormalMode => routes.CheckYourAnswersController.onPageLoad(srn)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }

  private[controllers] def addLprIndividualAddress(userAnswers: UserAnswers, address: LprAddress): UserAnswers =
    userAnswers.data
      .setObject(
        LprIndividualAddressPage.path,
        individualWithoutAddressFields(userAnswers) ++ Json.toJsObject(address)
      ) match {
      case JsSuccess(data, _) => userAnswers.copy(data = data)
      case _ => userAnswers
    }

  private def individualWithoutAddressFields(userAnswers: UserAnswers): JsObject =
    Seq(
      "addressLine1",
      "addressLine2",
      "addressLine3",
      "addressLine4",
      "ukPostcode",
      "country"
    ).foldLeft(
      (userAnswers.data \ "lprDetails" \ "individual")
        .asOpt[JsObject]
        .getOrElse(Json.obj())
    )(_ - _)
}
