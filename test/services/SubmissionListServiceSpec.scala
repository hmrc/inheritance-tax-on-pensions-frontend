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

package services

import play.api.test.FakeRequest
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import connectors.InheritanceTaxOnPensionsConnector
import config.FrontendAppConfig
import base.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import models.{IhtpOverviewResponse, IhtpOverviewSuccess}
import models.requests.AllowedAccessRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

class SubmissionListServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestNoEstablishersGen(FakeRequest()).sample.value

  "getSubmissionList" - {

    "must return success if connector returns success" in new Setup {
      val response = IhtpOverviewResponse(IhtpOverviewSuccess(allowedAccessRequest.schemeDetails.pstr, Seq.empty))

      when(mockConnector.getSubmissionList(any(), any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(response)))

      whenReady(testService.getSubmissionList()) {
        _ mustBe Right(response)
      }

      verify(mockConnector).getSubmissionList(
        eqTo(allowedAccessRequest.schemeDetails.pstr),
        eqTo("1900-01-01"),
        eqTo("9999-12-31"),
        any(),
        any(),
        any(),
        any()
      )(using any())
    }

    "must return error if connector returns error" in new Setup {
      val errorResponse = UpstreamErrorResponse("Retrieval failed", 500)

      when(mockConnector.getSubmissionList(any(), any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Left(errorResponse)))

      whenReady(testService.getSubmissionList()) {
        _ mustBe Left(errorResponse)
      }
    }
  }

  class Setup {
    val mockConnector: InheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
    val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

    when(mockAppConfig.submissionListDateFrom).thenReturn("1900-01-01")
    when(mockAppConfig.submissionListDateTo).thenReturn("9999-12-31")

    val testService: SubmissionListService = new SubmissionListService(mockConnector, mockAppConfig)
  }
}
