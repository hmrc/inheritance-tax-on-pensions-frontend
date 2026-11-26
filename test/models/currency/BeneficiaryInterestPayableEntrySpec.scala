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

package models.currency

import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import base.SpecBase
import models.beneficiary.BeneficiaryType
import controllers.beneficiary.routes
import models.{CheckMode, JourneyRole, NormalMode}
import pages.beneficiary._

class BeneficiaryInterestPayableEntrySpec extends SpecBase {
  private val entry = BeneficiaryInterestPayableEntry(testIndex)

  "BeneficiaryInterestPayableEntry" - {
    "must use BeneficiaryInterestPayablePage" in {
      entry.page mustBe BeneficiaryInterestPayablePage(testIndex)
    }

    "must use the beneficiaryInterestPayable message prefix" in {
      entry.messagePrefix mustBe "beneficiaryInterestPayable"
    }

    "must be allowed for a named individual beneficiary" in {
      entry.isAllowed(individualBeneficiaryUserAnswers) mustBe true
    }

    "must be allowed for a named trust beneficiary" in {
      val answers = emptyUserAnswers
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value
        .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Trust)
        .success
        .value
        .set(BeneficiaryTrustNamePage(testIndex), trustName)
        .success
        .value
      entry.isAllowed(answers) mustBe true
    }

    "must not be allowed for out-of-range indexes" in {
      testInvalidBeneficiaryIndexes.foreach { index =>
        BeneficiaryInterestPayableEntry(index).isAllowed(individualBeneficiaryUserAnswers) mustBe false
      }
    }

    "must not be allowed when beneficiaries are not known" in {
      val answers = individualBeneficiaryUserAnswers
        .set(AreBeneficiariesKnownPage, false)
        .success
        .value
      entry.isAllowed(answers) mustBe false
    }

    "must be allowed when areBeneficiariesKnown is unanswered and didPrSubmit is false" in {
      val answers = individualBeneficiaryUserAnswers
        .remove(AreBeneficiariesKnownPage)
        .success
        .value
        .set(DidPrSubmitPage, false)
        .success
        .value
      entry.isAllowed(answers) mustBe true
    }

    "must not be allowed when areBeneficiariesKnown is unanswered and didPrSubmit is true" in {
      val answers = individualBeneficiaryUserAnswers
        .remove(AreBeneficiariesKnownPage)
        .success
        .value
        .set(DidPrSubmitPage, true)
        .success
        .value
      entry.isAllowed(answers) mustBe false
    }

    "must not be allowed when neither areBeneficiariesKnown nor didPrSubmit is answered" in {
      val answers = individualBeneficiaryUserAnswers
        .remove(AreBeneficiariesKnownPage)
        .success
        .value
      entry.isAllowed(answers) mustBe false
    }

    "must not be allowed when the beneficiary name is missing" in {
      val answers = individualBeneficiaryUserAnswers
        .remove(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual))
        .success
        .value
      entry.isAllowed(answers) mustBe false
    }

    "must pass the individual display name as a message argument" in {
      entry.messageArgs(individualBeneficiaryUserAnswers) mustBe Seq(individualNameFormatted)
    }

    "must pass the trust name as a message argument" in {
      val answers = emptyUserAnswers
        .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Trust)
        .success
        .value
        .set(BeneficiaryTrustNamePage(testIndex), trustName)
        .success
        .value
      entry.messageArgs(answers) mustBe Seq(trustName)
    }

    "must pass no message arguments when the name is missing" in {
      entry.messageArgs(emptyUserAnswers) mustBe Seq.empty
    }

    "must submit to BeneficiaryInterestPayableController" in {
      Seq(NormalMode, CheckMode).foreach { mode =>
        entry.submitAction(srn, mode) mustBe
          routes.BeneficiaryInterestPayableController.onSubmit(srn, testIndex, mode)
      }
    }

    "must continue to the beneficiary list in NormalMode" in {
      entry.nextPage(srn, NormalMode, individualBeneficiaryUserAnswers) mustBe
        routes.BeneficiaryListController.onPageLoad(srn)
    }

    "must continue to check your answers in CheckMode" in {
      entry.nextPage(srn, CheckMode, individualBeneficiaryUserAnswers) mustBe
        controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }
  }
}
