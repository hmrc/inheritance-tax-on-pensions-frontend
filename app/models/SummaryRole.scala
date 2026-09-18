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

sealed trait SummaryRole {
  val name: String
  val key: String
}

object SummaryRole extends Enumerable.Implicits {

  case object CheckYourAnswers extends WithName("cya") with SummaryRole {
    override val key: String = "checkYourAnswers"
  }

  case object Continue extends WithName("continue") with SummaryRole {
    override val key: String = "continue"
  }

  val values: Seq[SummaryRole] = Seq(CheckYourAnswers, Continue)

  implicit val enumerable: Enumerable[SummaryRole] =
    Enumerable(values.map(journeyRole => journeyRole.toString -> journeyRole)*)

  implicit val jsLiteral: JavascriptLiteral[SummaryRole] = (journeyRole: SummaryRole) => journeyRole.name
}
