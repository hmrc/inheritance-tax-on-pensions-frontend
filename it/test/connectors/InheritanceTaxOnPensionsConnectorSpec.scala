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

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.{MinimalDetails, SchemeDetails, UserAnswers}
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneId}

class InheritanceTaxOnPensionsConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def applicationBuilder(userAnswers: Option[UserAnswers] = None, isPsa: Boolean = true,
                                            schemeDetails: SchemeDetails = defaultSchemeDetails,
                                            minimalDetails: MinimalDetails = defaultMinimalDetails
                                           ): GuiceApplicationBuilder = {
    super.applicationBuilder(userAnswers).configure("microservice.services.inheritanceTaxOnPensions.port" -> wireMockPort)
  }

  val id = "some_id"
  val fetchUrl = s"/inheritance-tax-on-pensions/user-answers/$id"
  val setUrl = "/inheritance-tax-on-pensions/user-answers"
  val clock: Clock    = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("Europe/London"))

  val userAnswers: UserAnswers = UserAnswers(
    id = id,
    data = JsObject(Seq("inputPagePlaceholder" -> Json.toJson("placeholder"))),
    lastUpdated = Instant.now(clock)
  )

  def connector(implicit app: Application): InheritanceTaxOnPensionsConnector = injected[InheritanceTaxOnPensionsConnector]

  "InheritanceTaxOnPensionsConnector" - {

    "get must" - {
      "successfully fetch user answers" in runningApplication { implicit app =>
        val jsonResponse: String = Json.toJson(userAnswers).toString()
        wireMockServer.stubFor(
          get(urlMatching(fetchUrl))
            .willReturn(aResponse().withStatus(OK).withBody(jsonResponse))
        )

        whenReady(connector.fetchUserAnswers(id)) { result =>
          result mustBe Right(userAnswers)
        }
      }

      "return an error when the upstream service returns an error" in runningApplication { implicit app =>
        wireMockServer.stubFor(
          get(urlMatching(fetchUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        whenReady(connector.fetchUserAnswers(id)) { result =>
          result.isLeft mustBe true
          result.swap.toOption.get.statusCode mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "set must" - {
      "successfully write user answers" in runningApplication { implicit app =>
        wireMockServer.stubFor(
          put(urlMatching(setUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(userAnswers))))
            .willReturn(aResponse().withStatus(OK))
        )

        whenReady(connector.setUserAnswers(userAnswers)) { result =>
          result.status mustBe OK
        }
      }

      "fail to write user answers when the service returns an error" in runningApplication { implicit app =>
        wireMockServer.stubFor(
          put(urlMatching(setUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(userAnswers))))
            .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
        )

        whenReady(connector.setUserAnswers(userAnswers)) { result =>
          result.status mustBe SERVICE_UNAVAILABLE
        }
      }
    }
  }
}
