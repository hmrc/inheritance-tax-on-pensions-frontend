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

package pages

import play.api.libs.json.JsPath
import models.{JourneyRole, PrType, UserAnswers}

import scala.util.Try

case object PrTypePage extends QuestionPage[PrType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "prType"

  override def cleanup(value: Option[PrType], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(PrType.Organisation) => userAnswers.remove(IndividualNamePage(JourneyRole.PrIndividual))
      case Some(PrType.Individual) => userAnswers.remove(IndividualNamePage(JourneyRole.PrOrganisation))
      case _ => super.cleanup(value, userAnswers)
    }
}
