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

package models

import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait LprType

object LprType extends Enumerable.Implicits {

  case object Individual extends WithName("individual") with LprType
  case object Organisation extends WithName("organisation") with LprType

  val values: Seq[LprType] = Seq(
    Individual,
    Organisation
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"lprType.${value.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index"),
      hint = if (value == Organisation) {
        Some(
          uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint(content = Text(messages("lprType.organisation.hint")))
        )
      } else {
        None
      }
    )
  }

  implicit val enumerable: Enumerable[LprType] =
    Enumerable(values.map(v => v.toString -> v)*)
}
