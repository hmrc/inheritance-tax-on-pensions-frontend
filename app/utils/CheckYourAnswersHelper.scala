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

package utils

import play.api.mvc.Call
import pages.{HasNinoPage, IndividualNamePage, NinoPage}
import controllers.routes
import models.SchemeId.Srn
import models.{JourneyRole, NormalMode, UserAnswers}

object CheckYourAnswersHelper {

  def findPageToContinue(answers: UserAnswers, srn: Srn): Option[Call] = {
    if (answers.get(IndividualNamePage(JourneyRole.Deceased)).isEmpty) {
      Some(routes.IndividualNameController.onPageLoad(srn, NormalMode, JourneyRole.Deceased))
    } else if (answers.get(HasNinoPage).isEmpty) {
      Some(routes.HasNinoController.onPageLoad(srn, NormalMode))
    } else if (answers.get(HasNinoPage).nonEmpty && answers.get(HasNinoPage).getOrElse(false) && answers.get(NinoPage).isEmpty) {
      Some(routes.NinoController.onPageLoad(srn, NormalMode))
    } else {
      None
    }
  }
}
