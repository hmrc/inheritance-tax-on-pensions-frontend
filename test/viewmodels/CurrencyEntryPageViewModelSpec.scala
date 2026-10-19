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

package viewmodels

import models.{CheckMode, JourneyRole}
import pages.beneficiary.BeneficiaryNamePage
import play.api.i18n.Messages
import play.api.test.Helpers.running
import org.mockito.Mockito.{spy, when}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase
import models.currency.{BeneficiaryIhtPayableEntry, IhtPayableEntry}

class CurrencyEntryPageViewModelSpec extends SpecBase {
  "CurrencyEntryPageViewModel" - {
    "must resolve parameterised content and the entry's form action" in {
      val app = applicationBuilder().build()
      running(app) {
        given Messages = messages(app)
        val entry = BeneficiaryIhtPayableEntry(0)
        val answers = emptyUserAnswers
          .set(BeneficiaryNamePage(0, JourneyRole.BeneficiaryIndividual), individualName)
          .success
          .value
        val page = CurrencyEntryPageViewModel(entry, answers, srn, CheckMode)
        page.title mustBe messages(app)("beneficiaryIhtPayable.title", individualNameFormatted)
        page.heading mustBe messages(app)("beneficiaryIhtPayable.heading", individualNameFormatted)
        page.hint mustBe Some(Text(messages(app)("beneficiaryIhtPayable.hint")))
        page.submitAction mustBe entry.submitAction(srn, CheckMode)
      }
    }

    "must omit a hint when no hint message is defined" in {
      val app = applicationBuilder().build()
      running(app) {
        val testMessages = spy(messages(app))
        when(testMessages.isDefinedAt("ihtPayable.hint")).thenReturn(false)
        given Messages = testMessages

        CurrencyEntryPageViewModel(IhtPayableEntry, emptyUserAnswers, srn, CheckMode).hint mustBe None
      }
    }
  }
}
