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

package controllers.testonly

import services.UserAnswersService
import play.api.mvc._
import controllers.IhtpBaseController
import models.SchemeId.Srn
import controllers.actions._
import models.PrAddress
import pages.beneficiary.{BeneficiaryAddressPage, BeneficiaryTypePage}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryAddressController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val address = PrAddress("1 ABCDE Street", None, None, Some("FGHIJ Town"), Some("AA1 1AA"), "GB")

  def seed(srn: Srn, index: Int): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        if (
          index < 0 || index >= 30 || request.userAnswers.uuid.trim.isEmpty ||
          request.userAnswers.get(BeneficiaryTypePage(index)).isEmpty
        ) {
          Future.successful(BadRequest("An existing report with details for the selected beneficiary is required"))
        } else {
          request.userAnswers
            .set(BeneficiaryAddressPage(index), address)
            .toEither
            .fold(
              _ => Future.successful(BadRequest("Unable to update beneficiary details")),
              updatedAnswers =>
                userAnswersService.set(updatedAnswers)(using hc, request.request).map {
                  _.fold(
                    _ => InternalServerError("Unable to save the test beneficiary address"),
                    _ => Ok("Address created")
                  )
                }
            )
        }
      }
}
