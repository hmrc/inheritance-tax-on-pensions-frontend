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
import models.SchemeId.Srn
import models.addresslookup.{AlfAddressData, AlfJourneyConfig}
import uk.gov.hmrc.http._
import models.{JourneyRole, Mode}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AddressLookupFrontendConnector @Inject() (
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances {

  def initJourney(srn: Srn, mode: Mode, journeyConfig: AlfJourneyConfig, journeyRole: JourneyRole)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    httpClient
      .post(url"${config.addressLookupFrontendBaseUrl}/api/init")
      .withBody(Json.toJson(journeyConfig))
      .execute[HttpResponse]
      .map(_.header("Location").getOrElse(config.addressLookupContinueUrl(srn, mode, journeyRole)))

  def getAddress(addressId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData] =
    httpClient
      .get(url"${config.addressLookupFrontendBaseUrl}/api/confirmed?id=$addressId")
      .execute[AlfAddressData]
}
