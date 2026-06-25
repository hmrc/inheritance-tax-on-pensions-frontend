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

import org.scalatest.freespec.AnyFreeSpec
import pages.{IndividualNamePage, LprTypePage}
import utils.LprNameHelper.fromUserAnswers
import base.SpecBase
import models.{IndividualName, JourneyRole, LprType}

class LprNameHelperSpec extends AnyFreeSpec with SpecBase {

  "when inidividual" - {
    "fromUserAnswers" - {

      "must return the lpr first name and surname if inividual when the name has been answered" in {

        val userAnswers = emptyUserAnswers
          .set(LprTypePage, LprType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.LprIndividual),
            IndividualName(
              title = Some("Dr"),
              firstForename = "John",
              secondForename = Some("William"),
              surname = "Doe"
            )
          )
          .get

        fromUserAnswers(userAnswers) mustBe Some("John Doe")
      }
      "must return None when the LPR name has not been answered" in {

        fromUserAnswers(emptyUserAnswers) mustBe None
      }
    }

    "withName" - {

      "must run the success block when the LPR name has been answered" in {

        val userAnswers = emptyUserAnswers
          .set(
            IndividualNamePage(JourneyRole.LprIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "John",
              secondForename = Some("William"),
              surname = "Doe"
            )
          )
          .success
          .value

        LprNameHelper.withName(userAnswers)("missing")(name => s"found $name") mustBe "found John Doe"
      }

      "must run the fallback block when the individual name has not been answered" in {

        LprNameHelper.withName(emptyUserAnswers)("missing")(name => s"found $name") mustBe "missing"
      }
    }
  }
  "when organisation" - {
    "fromUserAnswers" - {
      "must return the PR first name and surname when the organisation PR name has been answered" in {
        val userAnswers = emptyUserAnswers
          .set(LprTypePage, LprType.Organisation)
          .get
          .set(
            IndividualNamePage(JourneyRole.LprOrganisation),
            IndividualName(Some("Mrs"), "Sarah", Some("Jane"), "Wilson")
          )
          .get

        fromUserAnswers(userAnswers) mustBe Some("Sarah Wilson")
      }

      "must return None when the organisation PR name has not been answered" in {
        val userAnswers = emptyUserAnswers.set(LprTypePage, LprType.Organisation).get

        fromUserAnswers(userAnswers) mustBe None
      }
    }
  }
}
