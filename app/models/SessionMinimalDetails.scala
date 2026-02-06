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

import play.api.libs.json.{Format, OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.libs.functional.syntax._

import java.time.Instant

case class SessionMinimalDetails(
                                 id: String, // TODO - what is the correct session cache key for this?
                                 minimalDetails: MinimalDetails,
                                 lastUpdated: Instant = Instant.now
                               ) {

}

object SessionMinimalDetails {
  implicit val format: OFormat[SessionMinimalDetails] =
    (
      (__ \ "_id").format[String] and
        (__ \ "minimalDetails").format[MinimalDetails] and
        (__ \ "lastUpdated").format(using MongoJavatimeFormats.instantFormat)
      )(SessionMinimalDetails.apply, o => Tuple.fromProductTyped(o))
}
