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
import utils.DeceasedNameHelper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.HasNinoPage
import controllers.actions._
import forms.HasNinoFormProvider
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import views.html.HasNinoView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class HasNinoController @Inject() (
  override val messagesApi: MessagesApi,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasNinoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: HasNinoView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        DeceasedNameHelper.withName(request.userAnswers)(
          logAndJourneyRecovery("Deceased name is missing, cannot load the deceased NINO question page")
        ) { deceasedName =>
          val preparedForm = request.userAnswers.get(HasNinoPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, srn, mode, deceasedName))
        }
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        DeceasedNameHelper.withName(request.userAnswers)(
          Future.successful(
            logAndJourneyRecovery("Deceased name is missing, cannot submit the deceased NINO question page")
          )
        ) { deceasedName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode, deceasedName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNinoPage, value))
                  _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                } yield Redirect(nextPage(srn, mode, value))
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode, hasNino: Boolean) =
    if (hasNino) {
      routes.NinoController.onPageLoad(srn, mode)
    } else {
      routes.NoNinoReasonController.onPageLoad(srn, mode)
    }
}
