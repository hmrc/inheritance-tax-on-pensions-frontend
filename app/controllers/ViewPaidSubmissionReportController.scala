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

import services.{CountryService, ReportRetrievalService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import utils.CheckYourAnswersHelper.buildSummaryLists
import controllers.actions._
import views.html.ViewPaidSubmissionReportView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class ViewPaidSubmissionReportController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ViewPaidSubmissionReportView,
  countryService: CountryService,
  reportRetrievalService: ReportRetrievalService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onViewPaid(srn: Srn, fbNumber: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      reportRetrievalService
        .getReportByFbNumber(fbNumber)
        .map {
          case Right(response) =>
            response.header("uuid") match {
              case Some(uuid) =>
                Redirect(routes.ViewPaidSubmissionReportController.onPageLoad(srn))
                  .withSession(request.session + ("uuid" -> uuid))
              case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
            }
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      val checkYourAnswersSummaryLists =
        buildSummaryLists(request.userAnswers, srn, countryService.nameForCode, messagesApi).asReadOnly

      Ok(
        view(
          srn,
          checkYourAnswersSummaryLists.deceasedDetailsSummaryList,
          checkYourAnswersSummaryLists.prDetailsSummaryList,
          checkYourAnswersSummaryLists.paymentNoticeDetailsSummaryList,
          checkYourAnswersSummaryLists.beneficiaryList,
          request.request.schemeDetails.schemeName
        )
      )
    }
}
