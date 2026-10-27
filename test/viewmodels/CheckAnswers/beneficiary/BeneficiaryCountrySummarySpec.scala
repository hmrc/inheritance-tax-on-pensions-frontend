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

package viewmodels.CheckAnswers.beneficiary

import services.CountryService
import models.beneficiary.BeneficiaryType
import models.{CheckMode, JourneyRole}
import pages.beneficiary._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryCountrySummarySpec extends SpecBase {
  "BeneficiaryCountrySummary" - {
    "must return None when no address is saved" in {
      runningApplication { app =>
        implicit val messages: Messages = this.messages(app)
        BeneficiaryCountrySummary.row(srn, 0, emptyUserAnswers, identity) mustBe None
      }
    }

    Seq(BeneficiaryType.Individual -> "GB", BeneficiaryType.Trust -> "FR").foreach { (beneficiaryType, country) =>
      s"must resolve the country and restart ALF for the selected $beneficiaryType" in {
        val answers = (for {
          typed <- individualBeneficiaryUserAnswers.set(BeneficiaryTypePage(1), beneficiaryType)
          named <-
            if (beneficiaryType == BeneficiaryType.Trust) typed.set(BeneficiaryTrustNamePage(1), trustName)
            else typed.set(BeneficiaryNamePage(1, JourneyRole.BeneficiaryIndividual), individualName)
          addressed <- named.set(BeneficiaryAddressPage(1), testPrAddress.copy(country = country))
        } yield addressed).success.value
        val name = if (beneficiaryType == BeneficiaryType.Trust) trustName else individualNameFormatted

        runningApplication { app =>
          implicit val messages: Messages = this.messages(app)
          val countryService = app.injector.instanceOf[CountryService]
          val row = BeneficiaryCountrySummary.row(srn, 1, answers, countryService.nameForCode).value
          row.key.content mustBe Text(messages("beneficiaryCountry.checkYourAnswersLabel"))
          row.value.content mustBe Text(countryService.nameForCode(country))
          val action = row.actions.value.items.head
          action.href mustBe controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, 1, CheckMode).url
          action.visuallyHiddenText mustBe Some(messages("beneficiaryCountry.change.hidden", name))
        }
      }
    }
  }
}
