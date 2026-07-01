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

import services.AddressLookupFrontendService
import pages.IndividualNamePage
import models.SchemeId.Srn
import controllers.actions._
import models.{JourneyRole, Mode}
import uk.gov.hmrc.http.HttpVerbs.GET
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AddressLookupStartController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  addressLookupFrontendService: AddressLookupFrontendService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def start(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        request.userAnswers.get(IndividualNamePage(JourneyRole.LprIndividual)) match {
          case Some(lprIndividualName) =>
            addressLookupFrontendService
              .initJourney(srn, mode, s"${lprIndividualName.firstForename} ${lprIndividualName.surname}")
              .map(addressLookupUrl => Redirect(Call(GET, addressLookupUrl)))

          case None =>
            Future.successful(logAndJourneyRecovery("individual name is missing, cannot initialise address lookup"))
        }
      }
}
