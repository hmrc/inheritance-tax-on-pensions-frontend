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

package controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.IdentifierAction
import repositories.SessionSchemeDetailsRepository
import views.html.JourneyRecoveryStartAgainView
import models.SchemeId.Srn
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.Locale
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class JourneyRecoveryController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  view: JourneyRecoveryStartAgainView,
  schemeDetailsRepository: SessionSchemeDetailsRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val timeFormatter = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.UK)
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    schemeDetailsRepository.get(request.getUserId).map { schemeDetails =>
      val continueUrl = schemeDetails
        .flatMap(details => Srn(details.srn))
        .map(srn => routes.SubmissionListController.onPageLoad(srn).url)
        .getOrElse(throw new IllegalStateException("A valid cached scheme reference is required for journey recovery"))
      val now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))

      Ok(view(continueUrl, now.format(timeFormatter).toLowerCase(Locale.UK), now.format(dateFormatter)))
    }
  }
}
