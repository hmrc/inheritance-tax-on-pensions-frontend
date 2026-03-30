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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import forms.PspDeclarationFormProvider
import views.html.PspDeclarationView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class PspDeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider, // Invalidate the authorisation cache on declaration
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PspDeclarationFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PspDeclarationView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify
    .andThen(allowAccess(srn))
    .andThen(getData)
    .andThen(requireData) { implicit request =>
      val form = formProvider(request.request.schemeDetails.authorisingPSAID)
      Ok(view(form, srn, request.request.schemeDetails.schemeName))
    }

  def onSubmit(srn: Srn): Action[AnyContent] = identify
    .andThen(allowAccess(srn))
    .andThen(getData)
    .andThen(requireData) { implicit request =>
      val form = formProvider(request.request.schemeDetails.authorisingPSAID)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(view(formWithErrors, srn, request.request.schemeDetails.schemeName)),
          _ =>
            // TODO - correctly route when next step of the journey is built
            Redirect(routes.SubmissionListController.onPageLoad(srn))
        )
    }
}
