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

import play.api.test.FakeRequest
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import connectors.InheritanceTaxOnPensionsConnector
import base.SpecBase
import models.UserAnswers
import models.requests.{AllowedAccessRequest, OptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value

  class Harness(inheritanceTaxOnPensionsConnector: InheritanceTaxOnPensionsConnector)
      extends DataRetrievalActionImpl(inheritanceTaxOnPensionsConnector) {
    def callTransform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {
      val mockUpstreamErrorResponse = mock[UpstreamErrorResponse]
      when(mockUpstreamErrorResponse.statusCode).thenReturn(NOT_FOUND)

      val inheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(inheritanceTaxOnPensionsConnector.fetchUserAnswers(any())(using any()))
        .thenReturn(Future(Left(mockUpstreamErrorResponse)))
      val action = new Harness(inheritanceTaxOnPensionsConnector)

      "must set userAnswers to new instance in the request" in {
        val result = action.callTransform(request).futureValue
        result.userAnswers mustBe defined
      }
    }

    "when there is data in the cache" - {

      val inheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(inheritanceTaxOnPensionsConnector.fetchUserAnswers(any())(using any()))
        .thenReturn(Future(Right(UserAnswers("id"))))
      val action = new Harness(inheritanceTaxOnPensionsConnector)

      "must build a userAnswers object and add it to the request" in {
        val result = action.callTransform(request).futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
