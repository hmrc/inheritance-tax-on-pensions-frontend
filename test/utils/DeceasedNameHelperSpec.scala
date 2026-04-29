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
import pages.NameOfDeceasedPage
import base.SpecBase
import utils.DeceasedNameHelper.fromUserAnswers
import models.NameOfDeceased

class DeceasedNameHelperSpec extends AnyFreeSpec with SpecBase {

  "fromUserAnswers" - {

    "must return the deceased first name and surname when the name has been answered" in {

      val userAnswers = emptyUserAnswers
        .set(
          NameOfDeceasedPage,
          NameOfDeceased(
            title = Some("Dr"),
            firstForename = "John",
            secondForename = Some("William"),
            surname = "Doe"
          )
        )
        .success
        .value

      fromUserAnswers(userAnswers) mustBe Some("John Doe")
    }

    "must return None when the deceased name has not been answered" in {

      fromUserAnswers(emptyUserAnswers) mustBe None
    }
  }

  "withName" - {

    "must run the success block when the deceased name has been answered" in {

      val userAnswers = emptyUserAnswers
        .set(
          NameOfDeceasedPage,
          NameOfDeceased(
            title = Some("Mr"),
            firstForename = "John",
            secondForename = Some("William"),
            surname = "Doe"
          )
        )
        .success
        .value

      DeceasedNameHelper.withName(userAnswers)("missing")(name => s"found $name") mustBe "found John Doe"
    }

    "must run the fallback block when the deceased name has not been answered" in {

      DeceasedNameHelper.withName(emptyUserAnswers)("missing")(name => s"found $name") mustBe "missing"
    }
  }
}
