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

import services.ReportRetrievalService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.{Constants, FrontendAppConfig}
import config.Constants._
import controllers.actions._
import play.api.i18n.MessagesApi
import views.html.ConfirmationView
import models.SchemeId.Srn

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class ConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  appConfig: FrontendAppConfig,
  view: ConfirmationView,
  reportRetrievalService: ReportRetrievalService
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify
    .andThen(allowAccess(srn))
    .andThen(getData) { implicit request =>
      val schemeName = request.request.schemeDetails.schemeName
      (
        request.session.get(PAYMENT_REFERENCE),
        request.session.get(DUE_DATE),
        request.session.get(FORM_BUNDLE_NO),
        request.session.get(NEED_TO_PAY)
      ) match {
        case (Some(paymentReference), Some(dueDate), Some(formBundleNo), Some(needToPay)) =>
          Ok(
            view(
              paymentReference,
              request.request.minimalDetails.email.decryptedValue,
              srn,
              appConfig.schemeDashboardUrl(srn, request.request.pensionSchemeId),
              dueDate,
              schemeName,
              needToPay,
              formBundleNo
            )
          )
        case _ =>
          logAndJourneyRecovery("Unable to show submission confirmation page, session values are missing")
      }
    }

  def onChange(srn: Srn, formBundleNo: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      reportRetrievalService
        .getReportByFbNumber(formBundleNo)
        .map {
          case Right(response) =>
            val uuid = response.header("uuid").get
            val updatedSession = if (request.session.get("uuid").contains(uuid)) {
              request.session
            } else {
              request.session + ("uuid" -> uuid)
            }

            Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(srn))
              .withSession(updatedSession)
              .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
