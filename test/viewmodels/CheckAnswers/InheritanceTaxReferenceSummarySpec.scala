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

import play.api.test.Helpers.stubMessages
import pages.{IHTPaymentReferencePage, InheritanceTaxReferencePage}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class InheritanceTaxReferenceSummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "InheritanceTaxReferenceSummary" - {

    "must return None when data is not present" in {
      val result = InheritanceTaxReferenceSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when the inheritance tax reference is present" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, "1234567890")
        .success
        .value

      val result = InheritanceTaxReferenceSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("inheritanceTaxReference.checkYourAnswersLabel"))
      result.value.value mustEqual Value(HtmlContent("1234567890"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.routes.InheritanceTaxReferenceController.onPageLoad(srn, CheckMode).url,
          Text("site.change"),
          visuallyHiddenText = Some("inheritanceTaxReference.change.hidden")
        )
      )
    }

    "must return a row when the inheritance tax reference is present with payment reference" in {
      val userAnswers = emptyUserAnswers
        .set(IHTPaymentReferencePage, "paymentRef")
        .get
        .set(InheritanceTaxReferencePage, "1234567890")
        .success
        .value

      val result = InheritanceTaxReferenceSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("inheritanceTaxReference.checkYourAnswersLabel"))
      result.value.value mustEqual Value(HtmlContent("1234567890"))
      result.value.actions.value.items must not contain ActionItem(
        controllers.routes.InheritanceTaxReferenceController.onPageLoad(srn, CheckMode).url,
        Text("site.change"),
        visuallyHiddenText = Some("inheritanceTaxReference.change.hidden")
      )
    }
  }
}
