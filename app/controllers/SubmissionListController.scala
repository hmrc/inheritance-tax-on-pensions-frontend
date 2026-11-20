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

import services.{LockService, ReportRetrievalService, SubmissionListService}
import utils.SubmissionListUtil
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.{Constants, FrontendAppConfig}
import controllers.actions.{AllowAccessActionProvider, IdentifierAction}
import views.html.SubmissionListView
import models.SchemeId.Srn
import forms.SearchFormProvider
import uk.gov.hmrc.http.HeaderCarrier
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import models.requests.AllowedAccessRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SubmissionListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider, // Invalidate the authorisation cache and re-authenticate
  formProvider: SearchFormProvider,
  submissionListService: SubmissionListService,
  reportRetrievalService: ReportRetrievalService,
  appConfig: FrontendAppConfig,
  submissionListUtil: SubmissionListUtil,
  view: SubmissionListView,
  lockService: LockService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      request.session.get("uuid").foreach { uuid =>
        lockService.releaseLock(s"${srn.value}-$uuid", request.getUserId).failed.foreach(_ => ())
      }
      buildSubmissionList(srn, Ok, form, None)
    }

  def onAmend(srn: Srn, uuid: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      lockService.acquireOrRenew(s"${srn.value}-$uuid", request.getUserId).map {
        case true =>
          val updatedSession = if (request.session.get("uuid").contains(uuid)) {
            request.session
          } else {
            request.session + ("uuid" -> uuid)
          }
          Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(srn))
            .withSession(updatedSession)
            .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
        case false =>
          Redirect(controllers.routes.LockedController.onPageLoad())
            .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
      }
    }

  def onChange(srn: Srn, ihtPaymentReference: String, ihtVersion: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      reportRetrievalService
        .getReport(ihtPaymentReference, ihtVersion)
        .flatMap {
          case Right(response) =>
            val uuid = response.header("uuid").get
            lockService.acquireOrRenew(s"${srn.value}-$uuid", request.getUserId).map {
              case true =>
                val updatedSession = if (request.session.get("uuid").contains(uuid)) {
                  request.session
                } else {
                  request.session + ("uuid" -> uuid)
                }
                Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(srn))
                  .withSession(updatedSession)
                  .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
              case false =>
                Redirect(controllers.routes.LockedController.onPageLoad())
                  .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
            }
          case Left(_) =>
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .async { implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => buildSubmissionList(srn, Ok, formWithErrors, None),
            value => buildSubmissionList(srn, Ok, form, Some(value))
          )
      }

  private def buildSubmissionList(srn: Srn, status: Status, form: Form[String], maybeSearchFieldValue: Option[String])(
    implicit
    request: AllowedAccessRequest[?],
    hc: HeaderCarrier
  ) =
    submissionListService
      .getSubmissionList(
        dateFrom = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
        dateTo = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
      )(using hc, request)
      .map { response =>
        submissionListUtil.prepare(response, paid = false, request.getQueryString("page")) match {
          case Right((reports, pagination)) =>
            status(
              view(
                form,
                srn,
                request.schemeDetails.schemeName,
                reports,
                pagination,
                appConfig.schemeDashboardUrl(srn, request.pensionSchemeId)
              )
            ).removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
              .removingFromSession("uuid")
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      }
}
