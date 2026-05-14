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

package models

import play.api.libs.json._

case class MinimalDetails(
  email: SensitiveString,
  isPsaSuspended: Boolean,
  organisationName: Option[String],
  individualDetails: Option[SensitiveIndividualDetails],
  rlsFlag: Boolean,
  deceasedFlag: Boolean
)

object MinimalDetails {
  implicit val reads: Reads[MinimalDetails] = {
    import SensitiveDetails.{sensitiveStringReads, sensitiveIndividualDetailsReads}
    Json.reads[MinimalDetails]
  }

  implicit val writes: Writes[MinimalDetails] = {
    import SensitiveDetails.{sensitiveStringWrites, sensitiveIndividualDetailsWrites}
    Json.writes[MinimalDetails]
  }

  def encryptedFormat(implicit
    crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter
  ): Format[MinimalDetails] = {
    import SensitiveDetails.{sensitiveStringFormat, sensitiveIndividualDetailsFormat}
    Format(Json.reads[MinimalDetails], Json.writes[MinimalDetails])
  }
}

case class IndividualDetails(
  firstName: String,
  middleName: Option[String],
  lastName: String
) {
  val fullName: String = s"$firstName ${middleName.fold("")(identity)} $lastName"
}

object IndividualDetails {
  implicit val reads: Reads[IndividualDetails] = Json.reads[IndividualDetails]
  implicit val writes: Writes[IndividualDetails] = Json.writes[IndividualDetails]
}
