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

package connectors

import org.mockito.Mockito._
import config.FrontendAppConfig
import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import models.UserAnswers
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

class InheritanceTaxOnPensionsConnectorSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  
  "fetchUserAnswers" - {
    "must successfully fetch user answers" in new SetUp {
      val expectedUserAnswers: UserAnswers = emptyUserAnswers
      val mockUrl = s"http://inheritance-tax-on-pensions/user-answers/$id"

      when(mockConfig.getUserAnswersUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, UserAnswers]](using any(), any()))
        .thenReturn(Future.successful(Right(expectedUserAnswers)))

      when(connector.httpClient.get(any())(using any())).thenReturn(requestBuilder)

      whenReady(connector.fetchUserAnswers(id)) {
        _ mustBe Right(expectedUserAnswers)
      }
    }
  }

  "setUserAnswers" - {
    "must successfully write user answers" in new SetUp {
      val expectedUserAnswers: UserAnswers = emptyUserAnswers

      val putUrl = "http://inheritance-tax-on-pensions/user-answers"

      when(mockConfig.setUserAnswersUrl()).thenReturn(putUrl)

      when(connector.httpClient.put(any())(using any())).thenReturn(requestBuilder)

      when(requestBuilder.withBody(eqTo(Json.toJson(expectedUserAnswers)))(using any(), any(), any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](using any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.setUserAnswers(expectedUserAnswers)

      verify(connector.httpClient, atLeastOnce).put(eqTo(url"$putUrl"))(using any())
    }
  }

  class SetUp {
    val id = "some_id"
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    val connector =
      new InheritanceTaxOnPensionsConnector(config = mockConfig, httpClient = httpClient)
  }
}
