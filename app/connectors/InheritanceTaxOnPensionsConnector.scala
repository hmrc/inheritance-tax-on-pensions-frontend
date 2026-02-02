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

package connectors

import config.FrontendAppConfig
import uk.gov.hmrc.http._
import models.UserAnswers
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class InheritanceTaxOnPensionsConnector @Inject() (
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances {

  def fetchUserAnswers(id: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, UserAnswers]] =
    httpClient
      .get(url"${config.getUserAnswersUrl(id)}")
      .execute[Either[UpstreamErrorResponse, UserAnswers]]

  def setUserAnswers(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .put(url"${config.setUserAnswersUrl()}")
      .setHeader("Csrf-Token" -> "nocheck")
      .withBody(Json.toJson(userAnswers))
      .execute[HttpResponse]
}
