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

package services

import config.Constants.{PSA, PSP}
import models.requests.AllowedAccessRequest

trait BaseService {

  private def loggedInUserNameOrBlank(implicit request: AllowedAccessRequest[?]): String =
    request.minimalDetails.individualDetails match {
      case Some(individual) => individual.fullName
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => orgName
          case None => ""
        }
    }

  def schemeName(implicit req: AllowedAccessRequest[?]): String = req.schemeDetails.schemeName
  def srnVal(implicit req: AllowedAccessRequest[?]): String = req.srn.value
  def role(implicit req: AllowedAccessRequest[?]): String = if (req.pensionSchemeId.isPSP) PSP else PSA

  def schemeAdministratorOrPractitionerName(implicit req: AllowedAccessRequest[?]): String =
    req.schemeDetails.establishers.headOption.fold(loggedInUserNameOrBlank)(e => e.name)
}
