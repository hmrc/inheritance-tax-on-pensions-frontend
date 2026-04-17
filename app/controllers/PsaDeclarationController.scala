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

import services.ReportSubmissionService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PsaDeclarationView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class PsaDeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider, // Invalidate the authorisation cache on declaration
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  reportSubmissionService: ReportSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: PsaDeclarationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify
    .andThen(allowAccess(srn))
    .andThen(getData)
    .andThen(requireData) { implicit request =>
      Ok(view(srn, request.request.schemeDetails.schemeName))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        reportSubmissionService.submitReport(request.userAnswers)(using hc, request.request).map {
          case Right(_) => Redirect(routes.ConfirmationController.onPageLoad(srn))
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      }
}
