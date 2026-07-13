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

import viewmodels.implicits._
import play.twirl.api.HtmlFormat
import pages.PrIndividualAddressPage
import controllers.routes
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models._
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object PrIndividualAddressSummary {

  def row(
    srn: SchemeId.Srn,
    answers: UserAnswers,
    countryNameForCode: String => String = identity
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PrIndividualAddressPage).map { answer =>
      val address = Seq(
        Some(answer.addressLine1),
        answer.addressLine2,
        answer.addressLine3,
        answer.addressLine4,
        answer.ukPostcode,
        Some(countryNameForCode(answer.country))
      ).flatten.map(line => HtmlFormat.escape(line).toString).mkString("<br>")

      SummaryListRowViewModel(
        key = "prIndividualAddress.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(HtmlFormat.raw(address))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            routes.AddressLookupStartController.start(srn, CheckMode).url
          ).withVisuallyHiddenText(messages("prIndividualAddress.checkYourAnswersLabel.hidden"))
        )
      )
    }
}
