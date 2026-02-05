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

package services

import play.api.test.FakeRequest
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import connectors.InheritanceTaxOnPensionsConnector
import base.SpecBase
import config.Constants.PREPOPULATION_FLAG
import models.UserAnswers
import models.requests.AllowedAccessRequest
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class UserAnswersServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val allowedAccessRequestPrePopulation: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestGen(
      FakeRequest()
        .withSession((PREPOPULATION_FLAG, "true"))
    ).sample.value

  "fetch" - {

    "must return a success if the operation was successful" in new Setup {
      val userAnswers: UserAnswers = emptyUserAnswers
      when(mockInheritanceTaxOnPensionsConnector.fetchUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswers)))

      whenReady(testService.fetch("id")) {
        _ mustBe Right(userAnswers)
      }
    }
    "must return an error if the operation was unsuccessful" in new Setup {
      val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse("Something went wrong", INTERNAL_SERVER_ERROR)
      when(mockInheritanceTaxOnPensionsConnector.fetchUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Left(errorResponse)))

      whenReady(testService.fetch("id")) {
        _ mustBe Left(errorResponse)
      }
    }
  }

  "set" - {

    "must return a success if the operation was successful" in new Setup {
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(HttpResponse(status = OK, body = "success response")))

      whenReady(testService.set(emptyUserAnswers)) {
        _.status mustBe OK
      }
    }
    "must return an error if the operation was unsuccessful" in new Setup {
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(HttpResponse(status = INTERNAL_SERVER_ERROR, body = "error response")))

      whenReady(testService.set(emptyUserAnswers)) {
        _.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  class Setup {
    val mockInheritanceTaxOnPensionsConnector: InheritanceTaxOnPensionsConnector =
      mock[InheritanceTaxOnPensionsConnector]
    val testService = new UserAnswersService(mockInheritanceTaxOnPensionsConnector)
  }
}
