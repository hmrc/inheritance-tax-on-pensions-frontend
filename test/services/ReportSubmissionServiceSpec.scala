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
import base.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import models.IhtpReportSubmissionResponse
import models.requests.AllowedAccessRequest
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

import java.time.Instant

class ReportSubmissionServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestNoEstablishersGen(FakeRequest()).sample.value

  "submitReport" - {

    "must return success if connector returns success" in new Setup {
      val response = IhtpReportSubmissionResponse(Instant.now(), "formBundle", "paymentRef")
      when(mockConnector.submitReport(any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(response)))

      whenReady(testService.submitReport("userAnswersId")) {
        _ mustBe Right(response)
      }
    }

    "must return error if connector returns error" in new Setup {
      val errorResponse = UpstreamErrorResponse("Submission failed", 500)
      when(mockConnector.submitReport(any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Left(errorResponse)))

      whenReady(testService.submitReport("userAnswersId")) {
        _ mustBe Left(errorResponse)
      }
    }
  }

  class Setup {
    val mockConnector: InheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
    val mockUserAnswersService: UserAnswersService = mock[UserAnswersService]
    val testService: ReportSubmissionService = new ReportSubmissionService(mockConnector, mockUserAnswersService)
  }
}
