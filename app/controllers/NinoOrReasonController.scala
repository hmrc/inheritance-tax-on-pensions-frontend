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
import pages.NinoOrReasonPage
import controllers.actions._
import forms.{NinoOrReasonFormData, NinoOrReasonFormProvider}
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.NinoOrReasonView
import models.SchemeId.Srn

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
    extends IhtpBaseController {

  val form: Form[NinoOrReasonFormData] = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        DeceasedNameHelper.withName(request.userAnswers)(
          logAndJourneyRecovery("missing deceased name, cannot load the page")
        ) { deceasedName =>
          val preparedForm = request.userAnswers.get(NinoOrReasonPage) match {
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
        DeceasedNameHelper.withName(request.userAnswers) {
          Future.successful(logAndJourneyRecovery("missing deceased name, cannot submit the page"))
        } { deceasedName =>
          val boundForm = formProvider.validate(form.bindFromRequest())

          boundForm.fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode, deceasedName))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(NinoOrReasonPage, value))
                _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield Redirect(nextPage(srn, mode))
          )
        }
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case NormalMode => routes.BirthDeathDatesController.onPageLoad(srn, mode)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
