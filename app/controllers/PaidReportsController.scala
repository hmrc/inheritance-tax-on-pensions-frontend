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
import utils.SubmissionListUtil
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Constants
import controllers.actions.{AllowAccessActionWithSessionCacheProvider, IdentifierAction}
import views.html.PaidReportsView
import models.SchemeId.Srn
import forms.SearchFormProvider
import uk.gov.hmrc.http.HeaderCarrier
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import models.requests.AllowedAccessRequest
import play.api.data.Form

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PaidReportsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  formProvider: SearchFormProvider,
  submissionListService: SubmissionListService,
  submissionListUtil: SubmissionListUtil,
  view: PaidReportsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      buildSubmissionList(srn, Ok, form, None)
    }

  def onSubmitSearch(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .async { implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => buildSubmissionList(srn, BadRequest, formWithErrors, Some(formWithErrors.data("value"))),
            value => buildSubmissionList(srn, Ok, form, Some(value))
          )
      }

  private def buildSubmissionList(
    srn: Srn,
    status: Status,
    form: Form[String],
    optionalSearchFieldValue: Option[String]
  )(implicit
    request: AllowedAccessRequest[?],
    hc: HeaderCarrier
  ) =
    submissionListService
      .getSubmissionList(
        dateFrom = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
        dateTo = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        searchFieldValue = optionalSearchFieldValue
      )(using hc, request)
      .map { response =>
        submissionListUtil.prepare(response, paid = true, request.getQueryString("page")) match {
          case Right((reports, pagination)) =>
            status(
              view(
                optionalSearchFieldValue match {
                  case Some(value) => form.fill(value)
                  case None => form
                },
                srn,
                request.schemeDetails.schemeName,
                reports,
                pagination
              )
            )
              .removingFromSession(Constants.SUBMITTED_PAGE_SESSION_VARIABLES*)
              .removingFromSession("uuid")
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      }
}
