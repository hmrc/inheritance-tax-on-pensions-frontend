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
import pages.PrTypePage
import controllers.IhtpBaseController
import models.SchemeId.Srn
import controllers.actions._
import play.api.libs.json._
import models.{PrAddress, RichJsObject, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class PrAddressController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val address = PrAddress("1 ABCDE Street", None, None, Some("FGHIJ Town"), Some("AA1 1AA"), "GB")
  private val addressFields =
    Seq("addressline1", "addressline2", "addressline3", "addressline4", "ukPostcode", "country")

  def seed(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        withTestAddress(request.userAnswers).fold(
          result => Future.successful(result),
          updatedAnswers =>
            userAnswersService.set(updatedAnswers)(using hc, request.request).map {
              _.fold(_ => InternalServerError("Unable to save the test PR address"), _ => Ok("Address created"))
            }
        )
      }

  private def withTestAddress(userAnswers: UserAnswers): Either[Result, UserAnswers] = {
    val missingDetails = BadRequest("An existing report with PR type and details is required")

    for {
      _ <- Either.cond(userAnswers.uuid.trim.nonEmpty, (), missingDetails)
      prType <- userAnswers.get(PrTypePage).toRight(missingDetails)
      key = prType.toString
      details <- (userAnswers.data \ "prDetails" \ key).asOpt[JsObject].toRight(missingDetails)
      updatedDetails = addressFields.foldLeft(details)(_ - _) ++ Json.toJsObject(address)
      data <- userAnswers.data
        .setObject(JsPath \ "prDetails" \ key, updatedDetails)
        .asEither
        .left
        .map(_ => BadRequest("Unable to update PR details"))
    } yield userAnswers.copy(data = data)
  }
}
