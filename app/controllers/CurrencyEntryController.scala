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
import controllers.actions._
import forms.CurrencyEntryFormProvider
import models.{CurrencyEntry, Mode}
import views.html.CurrencyEntryView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.CurrencyEntryPageViewModel

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class CurrencyEntryController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CurrencyEntryFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: CurrencyEntryView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode, entry: CurrencyEntry): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      if (entry.isAllowed(request.userAnswers)) {
        val form = formProvider(entry)
        val preparedForm = request.userAnswers.get(entry.page).fold(form)(form.fill)
        Ok(view(preparedForm, CurrencyEntryPageViewModel(entry, request.userAnswers, srn, mode)))
      } else {
        logAndJourneyRecovery(s"Invalid journey state for ${entry.messagePrefix}")
      }
    }

  def onSubmit(srn: Srn, mode: Mode, entry: CurrencyEntry): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      if (entry.isAllowed(request.userAnswers)) {
        formProvider(entry)
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(view(errors, CurrencyEntryPageViewModel(entry, request.userAnswers, srn, mode)))
              ),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(entry.page, value))
                saved <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield saved.fold(
                _ => logAndJourneyRecovery(s"Unable to save ${entry.messagePrefix}"),
                answers => Redirect(entry.nextPage(srn, mode, answers))
              )
          )
      } else {
        Future.successful(logAndJourneyRecovery(s"Invalid journey state for ${entry.messagePrefix}"))
      }
    }
}
