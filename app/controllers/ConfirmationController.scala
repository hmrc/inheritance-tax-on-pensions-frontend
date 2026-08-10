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
import pages.{IHTPaymentReferencePage, PaymentNoticeDatePage}
import config.FrontendAppConfig
import controllers.actions._
import views.html.ConfirmationView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  appConfig: FrontendAppConfig,
  view: ConfirmationView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify
    .andThen(allowAccess(srn))
    .andThen(getData) { implicit request =>
      val paymentReference = request.userAnswers
        .flatMap(_.get(IHTPaymentReferencePage))
        .getOrElse("")
      val dueDateFormatted = request.userAnswers
        .flatMap(_.get(PaymentNoticeDatePage))
        .getOrElse(LocalDate.now)
        .plusDays(35)
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
      val schemeName = request.request.schemeDetails.schemeName
      Ok(
        view(
          paymentReference,
          request.request.minimalDetails.email.decryptedValue,
          srn,
          appConfig.schemeDashboardUrl(srn, request.request.pensionSchemeId),
          dueDateFormatted,
          schemeName
        )
      )
    }
}
