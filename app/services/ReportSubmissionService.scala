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

import com.google.inject.Inject
import connectors.InheritanceTaxOnPensionsConnector
import pages.PaymentReferencePage
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import models.IhtpReportSubmissionResponse
import models.requests.AllowedAccessRequest

import scala.concurrent.Future
import scala.util.Success

class ReportSubmissionService @Inject() (
  inheritanceTaxOnPensionsConnector: InheritanceTaxOnPensionsConnector,
  userAnswersService: UserAnswersService
)(implicit ec: scala.concurrent.ExecutionContext)
    extends BaseService
    with Logging {

  def submitReport(
    userAnswersId: String
  )(implicit
    hc: HeaderCarrier,
    request: AllowedAccessRequest[?]
  ): Future[Either[UpstreamErrorResponse, IhtpReportSubmissionResponse]] =
    inheritanceTaxOnPensionsConnector
      .submitReport(
        request.schemeDetails.pstr,
        userAnswersId,
        schemeAdministratorOrPractitionerName,
        schemeName,
        srnVal,
        role
      )
      .andThen { case Success(Right(response)) =>
        userAnswersService.fetch(userAnswersId).foreach {
          case Right(userAnswers) =>
            userAnswers.set(PaymentReferencePage, response.paymentReference).foreach { updatedAnswers =>
              userAnswersService.set(updatedAnswers)
            }
          case Left(_) =>
            logger.warn(s"[ReportSubmissionService] Failed to fetch user answers - payment reference not saved")
        }
      }
}
