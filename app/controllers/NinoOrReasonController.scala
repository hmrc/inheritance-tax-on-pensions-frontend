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

import services.UserAnswersService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.NinoOrReasonPage
import controllers.actions._
import forms.{NinoOrReasonFormData, NinoOrReasonFormProvider}
import models.Mode
import play.api.data.Form
import views.html.NinoOrReasonView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class NinoOrReasonController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: NinoOrReasonFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: NinoOrReasonView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[NinoOrReasonFormData] = formProvider()

  private def deceasedName(implicit request: models.requests.DataRequest[?]): String =
    request.request.minimalDetails.individualDetails
      .map(_.fullName.trim.replaceAll("\\s+", " "))
      .orElse(request.request.minimalDetails.organisationName)
      .fold("")(identity)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        val preparedForm = request.userAnswers.get(NinoOrReasonPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, srn, mode, deceasedName))
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        val boundForm = formProvider.validate(form.bindFromRequest())

        boundForm.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode, deceasedName))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NinoOrReasonPage, value))
              _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
            } yield Redirect(routes.CheckYourAnswersController.onPageLoad(srn))
        )
      }
}
