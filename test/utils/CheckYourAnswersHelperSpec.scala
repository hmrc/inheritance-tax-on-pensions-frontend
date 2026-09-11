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

import base.SpecBase
import controllers.routes
import models.{CheckMode, IndividualName, JourneyRole, NormalMode}
import org.scalatest.freespec.AnyFreeSpec
import pages.{HasNinoPage, IndividualNamePage, InheritanceTaxReferencePage}

class CheckYourAnswersHelperSpec extends AnyFreeSpec with SpecBase {

  private val name = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Surname"
  )

  "findPageToContinue" - {

    "must return the deceased name page if a value is missing" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, inheritanceTaxReference)
        .success
        .value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(routes.IndividualNameController.onPageLoad(srn, NormalMode, JourneyRole.Deceased).url)
    }
    "must return the hasNino page if a value is missing" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, inheritanceTaxReference).success.value
        .set(IndividualNamePage(JourneyRole.Deceased), individualName).success.value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(routes.HasNinoController.onPageLoad(srn, NormalMode).url)
    }
  }
}
