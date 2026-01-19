/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{badRequest, notFound, ok}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.PensionSchemeUser.{Administrator, Practitioner}
import models.{PensionSchemeUser, SessionData, UserAnswers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDataCacheConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def applicationBuilder(userAnswers: Option[UserAnswers] = None, isPsa: Boolean = true): GuiceApplicationBuilder =
    super.applicationBuilder(userAnswers).configure("microservice.services.pensionAdministrator.port" -> wireMockPort)

  lazy val url = s"/pension-administrator/journey-cache/session-data-self"

  def stubGet(response: ResponseDefinitionBuilder): StubMapping =
    stubGet(url, response)

  def stubDelete(response: ResponseDefinitionBuilder): StubMapping =
    stubDelete(url, response)

  def response(pensionSchemeUser: PensionSchemeUser): String =
    s"""{"administratorOrPractitioner": "$pensionSchemeUser"}"""

  def okResponse(pensionSchemeUser: PensionSchemeUser): ResponseDefinitionBuilder =
    ok(response(pensionSchemeUser)).withHeader("Content-Type", "application/json")

  def connector(implicit app: Application): SessionDataCacheConnector = injected[SessionDataCacheConnector]

  "fetch" - {

    "return an administrator" in runningApplication { implicit app =>
      stubGet(okResponse(Administrator))

      connector.fetch().futureValue mustBe Some(SessionData(Administrator))
    }

    "return a practitioner" in runningApplication { implicit app =>
      stubGet(okResponse(Practitioner))

      connector.fetch().futureValue mustBe Some(SessionData(Practitioner))
    }

    "return none" in runningApplication { implicit app =>
      stubGet(notFound)

      connector.fetch().futureValue mustBe None
    }

    "return a failed future for bad request" in runningApplication { implicit app =>
      stubGet(badRequest)

      connector.fetch().failed.futureValue
    }
  }
}
