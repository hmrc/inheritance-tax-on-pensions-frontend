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

import play.api.mvc.ActionTransformer
import connectors.InheritanceTaxOnPensionsConnector
import play.api.Logging
import models.UserAnswers
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, OptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class DataRetrievalActionImpl @Inject() (
  val inheritanceTaxOnPensionsConnector: InheritanceTaxOnPensionsConnector
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with Logging {

  override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {

    val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    // TODO - We may wish to move this into a service and handle NOT_FOUND and HTTP exceptions? Journey recovery?
    // TODO - Do we need to think about the cache key?
    // TODO - When do we want to create the cache? Start of the journey?
    inheritanceTaxOnPensionsConnector
      .fetchUserAnswers(request.getUserId)(using headerCarrier)
      .map {
        case Right(ua) => OptionalDataRequest(request, Some(ua))
        case Left(ex) if ex.statusCode == NOT_FOUND =>
          logger.info("No user answers found - creating new user answers")
          OptionalDataRequest(request, Some(UserAnswers(request.getUserId)))
        case Left(ex) =>
          logger.warn("Data retrieval failed with upstream error response: ", ex)
          // TODO - we may want to return a Future[Either[Result, OptionalDataRequest[A]]] and go to journey recovery?
          throw new RuntimeException("Failed to fetch user answers from the cache")
      }
  }

}

trait DataRetrievalAction extends ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
