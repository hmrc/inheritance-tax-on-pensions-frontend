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

import services.SubmissionListService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions.{AllowAccessActionProvider, IdentifierAction}
import models.IhtpOverviewReport
import views.html.SubmissionListView
import models.SchemeId.Srn
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.SubmissionListPagination

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class SubmissionListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider, // Invalidate the authorisation cache and re-authenticate
  submissionListService: SubmissionListService,
  appConfig: FrontendAppConfig,
  view: SubmissionListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val pageSize = appConfig.submissionListPageSize

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      submissionListService.getSubmissionList().map {
        case Right(response) =>
          val (reports, pagination) = paginate(response.success.ihtpOverview)
          Ok(
            view(
              srn,
              request.schemeDetails.schemeName,
              reports,
              pagination,
              appConfig.schemeDashboardUrl(srn, request.pensionSchemeId)
            )
          )
        case Left(error) if error.statusCode == UNPROCESSABLE_ENTITY =>
          Ok(
            view(
              srn,
              request.schemeDetails.schemeName,
              Seq.empty,
              SubmissionListPagination(1, pageSize, 0),
              appConfig.schemeDashboardUrl(srn, request.pensionSchemeId)
            )
          )
        case Left(_) =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def paginate(
    reports: Seq[IhtpOverviewReport]
  )(implicit request: play.api.mvc.Request[?]): (Seq[IhtpOverviewReport], SubmissionListPagination) = {
    val totalPages = Math.max(1, Math.ceil(reports.size.toDouble / pageSize.toDouble).toInt)
    val currentPage = request.getQueryString("page").flatMap(_.toIntOption).getOrElse(1).max(1).min(totalPages)
    val firstItemIndex = (currentPage - 1) * pageSize

    reports.slice(firstItemIndex, firstItemIndex + pageSize) -> SubmissionListPagination(
      currentPage,
      pageSize,
      reports.size
    )
  }
}
