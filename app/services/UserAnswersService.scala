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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import models.UserAnswers
import models.requests.AllowedAccessRequest

import scala.concurrent.Future

class UserAnswersService @Inject() (inheritanceTaxOnPensionsConnector: InheritanceTaxOnPensionsConnector)
    extends BaseService {

  def fetch(
    id: String
  )(implicit hc: HeaderCarrier, request: AllowedAccessRequest[?]): Future[Either[UpstreamErrorResponse, UserAnswers]] =
    inheritanceTaxOnPensionsConnector.fetchUserAnswers(
      id,
      schemeAdministratorOrPractitionerName,
      schemeName,
      srnVal,
      role
    )

  def set(
    userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, request: AllowedAccessRequest[?]): Future[HttpResponse] =
    inheritanceTaxOnPensionsConnector.setUserAnswers(
      userAnswers,
      schemeAdministratorOrPractitionerName,
      schemeName,
      srnVal,
      role
    )
}
