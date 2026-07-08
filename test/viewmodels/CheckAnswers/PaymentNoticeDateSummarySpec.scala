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

package viewmodels.CheckAnswers

import pages.PaymentNoticeDatePage
import models.CheckMode
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

import java.time.LocalDate

class PaymentNoticeDateSummarySpec extends AnyFreeSpec with SpecBase {

  "PaymentNoticeDateSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {
      val result = PaymentNoticeDateSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row with the payment notice receipt date" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentNoticeDatePage, LocalDate.of(2026, 3, 27))
        .success
        .value

      val result = PaymentNoticeDateSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("paymentNoticeDate.checkYourAnswersLabel"))
      result.get.value.content mustBe Text("27 Mar 2026")
      result.get.actions.get.items.head.href mustBe
        controllers.routes.PaymentNoticeDateController.onPageLoad(srn, CheckMode).url
    }
  }
}
