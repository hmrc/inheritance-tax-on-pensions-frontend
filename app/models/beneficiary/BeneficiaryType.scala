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

package models.beneficiary

import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import models.Enumerable
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import play.api.i18n.Messages
import viewmodels.WithName

sealed trait BeneficiaryType

object BeneficiaryType extends Enumerable.Implicits {

  case object Individual extends WithName("individual") with BeneficiaryType
  case object Organisation extends WithName("organisation") with BeneficiaryType

  val values: Seq[BeneficiaryType] = Seq(
    Individual,
    Organisation
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"beneficiaryType.${value.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[BeneficiaryType] =
    Enumerable(values.map(v => v.toString -> v)*)
}
