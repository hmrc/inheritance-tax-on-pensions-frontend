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

import models.JourneyRole.PrIndividual
import org.scalatest.freespec.AnyFreeSpec
import pages._
import controllers.routes
import base.SpecBase
import play.api.libs.json.Json
import models.beneficiary.BeneficiaryType
import models._

class CheckYourAnswersHelperSpec extends AnyFreeSpec with SpecBase {

  "findPageToContinue" - {

    "must return the hasNino page if a value is missing" in {
      val userAnswers = emptyUserAnswers

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.HasNinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the nino page if a value is missing and hasNino is true" in {
      val userAnswers = emptyUserAnswers
        .set(HasNinoPage, true)
        .success
        .value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the noNinoReason page if a value is missing and hasNino is false" in {
      val userAnswers = emptyUserAnswers
        .set(HasNinoPage, false)
        .success
        .value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NoNinoReasonController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the individualName page if a value is missing" in {
      val userAnswers = emptyUserAnswers
        .set(HasNinoPage, false)
        .success
        .value
        .set(NoNinoReasonPage, "No Nino Reason")
        .get
        .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
        .get
        .set(PrTypePage, PrType.Individual)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrIndividual).url
      )
    }

    "must return the BeneficiaryName page if a value is missing" in {
      val userAnswers = emptyUserAnswers
        .copy(
          data = Json.obj(
            "prDetails" -> Json.obj(
              "individual" -> Json.obj(
                "title" -> "Ms",
                "firstForename" -> "Firstnametwo",
                "secondForename" -> "Middlenametwo",
                "surname" -> "Surname",
                "addressline1" -> "33 Fake Street",
                "addressline2" -> "AB Area",
                "addressline3" -> "Some District",
                "addressline4" -> "Anytown",
                "ukPostcode" -> "ZZ1 1ZZ",
                "country" -> "GB"
              )
            )
          )
        )
        .set(HasNinoPage, false)
        .get
        .set(NoNinoReasonPage, "No Nino Reason")
        .get
        .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
        .get
        .set(PrTypePage, PrType.Individual)
        .get
        .set(DidPrSubmitPage, true)
        .get
        .set(PaymentNoticeDatePage, testPaymentNoticeDate)
        .get
        .set(AreBeneficiariesKnownPage, true)
        .get
        .set(pages.beneficiary.BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode).url
      )
    }
  }
}
