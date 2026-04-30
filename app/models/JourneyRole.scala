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

import play.api.mvc.JavascriptLiteral

sealed trait JourneyRole {
  val name: String
  val key: String
}

object JourneyRole extends Enumerable.Implicits {

  case object Deceased extends WithName("deceased") with JourneyRole {
    override val key: String = "nameOfDeceased"
  }

  case object LprIndividual extends WithName("lpr-individual") with JourneyRole {
    override val key: String = "lprIndividualName"
  }

  case object Unknown extends WithName("unknown") with JourneyRole {
    override val key: String = "unknown"
  }

  val values: Seq[JourneyRole] = Seq(Deceased, LprIndividual)

  def withNameWithDefault(name: String): JourneyRole =
    values.find(_.toString.equalsIgnoreCase(name)).getOrElse(Unknown)

  implicit val enumerable: Enumerable[JourneyRole] =
    Enumerable(values.map(journeyRole => journeyRole.toString -> journeyRole)*)

  implicit val jsLiteral: JavascriptLiteral[JourneyRole] = (journeyRole: JourneyRole) => journeyRole.name
}
