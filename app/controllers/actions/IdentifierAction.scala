/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import play.api.mvc.*
import com.google.inject.Inject
import controllers.routes
import config.FrontendAppConfig
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import play.api.mvc.Results.*
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class IdentifierActionImpl @Inject()(
  config: FrontendAppConfig,
  override val authConnector: AuthConnector,
  sessionDataCacheConnector: SessionDataCacheConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId) {
        _.map { internalId =>
          block(IdentifierRequest(request, internalId))
        }.getOrElse(throw new UnauthorizedException("Unable to retrieve internal Id"))
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }
}

