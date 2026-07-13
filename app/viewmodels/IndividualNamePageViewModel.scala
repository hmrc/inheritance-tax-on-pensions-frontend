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

import uk.gov.hmrc.govukfrontend.views.viewmodels.button.Button
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.input.Input
import viewmodels.govuk.all._
import models.JourneyRole
import play.api.i18n.Messages
import play.api.data.Form
import viewmodels.InputWidth._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content

case class IndividualNamePageViewModel(
  title: String,
  heading: String,
  hint: Option[Content],
  inputs: Seq[Input],
  button: Button
)

object IndividualNamePageViewModel {

  def apply(
    form: Form[?],
    journeyRole: JourneyRole,
    organisationName: Option[String] = None
  )(implicit messages: Messages): IndividualNamePageViewModel =
    IndividualNamePageViewModel(
      title = pageMessage(s"${journeyRole.key}.title", journeyRole, organisationName),
      heading = pageMessage(s"${journeyRole.key}.heading", journeyRole, organisationName),
      hint = Option.when(messages.isDefinedAt(s"${journeyRole.key}.hint")) {
        Text(messages(s"${journeyRole.key}.hint"))
      },
      inputs = Seq(
        InputViewModel(
          field = form("title"),
          label = LabelViewModel(Text(messages("common.nameInput.title")))
            .withCssClass("govuk-label--s")
        )
          .withHint(HintViewModel(Text(messages("common.nameInput.titleHint"))))
          .withWidth(Fixed5),
        InputViewModel(
          field = form("firstForename"),
          label = LabelViewModel(Text(messages("common.nameInput.firstName")))
            .withCssClass("govuk-label--s")
        )
          .withWidth(ThreeQuarters),
        InputViewModel(
          field = form("secondForename"),
          label = LabelViewModel(Text(messages("common.nameInput.middleName")))
            .withCssClass("govuk-label--s")
        )
          .withWidth(ThreeQuarters),
        InputViewModel(
          field = form("surname"),
          label = LabelViewModel(Text(messages("common.nameInput.lastName")))
            .withCssClass("govuk-label--s")
        )
          .withWidth(ThreeQuarters)
      ),
      button = ButtonViewModel(Text(messages("site.saveAndContinue")))
    )

  private def pageMessage(
    messageKey: String,
    journeyRole: JourneyRole,
    organisationName: Option[String]
  )(implicit messages: Messages): String =
    journeyRole match {
      case JourneyRole.PrOrganisation => messages(messageKey, organisationName.getOrElse(""))
      case _ => messages(messageKey)
    }
}
