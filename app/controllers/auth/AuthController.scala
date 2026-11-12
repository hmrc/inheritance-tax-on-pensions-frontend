/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.auth

import services.LockService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class AuthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  sessionSchemeDetailsRepository: SessionSchemeDetailsRepository,
  sessionMinimalDetailsRepository: SessionMinimalDetailsRepository,
  identify: IdentifierAction,
  lockService: LockService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def signOut(): Action[AnyContent] = identify.async { implicit request =>
    for {
      schemeDetails <- sessionSchemeDetailsRepository.get(request.userId)
      _ <- sessionSchemeDetailsRepository.clear(request.userId)
      _ <- sessionMinimalDetailsRepository.clear(request.getUserId)
    } yield {
      for {
        srn <- schemeDetails.map(_.srn)
        uuid <- request.session.get("uuid")
      }
        lockService.releaseLock(s"$srn-$uuid", request.getUserId).failed.foreach(_ => ())
      Redirect(config.urls.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
    }
  }

  def signOutNoSurvey(): Action[AnyContent] = identify.async { implicit request =>
    for {
      schemeDetails <- sessionSchemeDetailsRepository.get(request.userId)
      _ <- sessionSchemeDetailsRepository.clear(request.userId)
      _ <- sessionMinimalDetailsRepository.clear(request.getUserId)
    } yield {
      for {
        srn <- schemeDetails.map(_.srn)
        uuid <- request.session.get("uuid")
      }
        lockService.releaseLock(s"$srn-$uuid", request.getUserId).failed.foreach(_ => ())
      Redirect(config.urls.signOutUrl, Map("continue" -> Seq(routes.SignedOutController.onPageLoad().url)))
    }
  }
}
