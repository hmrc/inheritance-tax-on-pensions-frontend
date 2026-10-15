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

import play.api.mvc.Call
import pages.QuestionPage
import models.SchemeId.Srn

import scala.annotation.unused

trait CurrencyEntry {
  def page: QuestionPage[BigDecimal]
  def messagePrefix: String
  def isAllowed(answers: UserAnswers): Boolean
  def submitAction(srn: Srn, mode: Mode): Call
  def nextPage(srn: Srn, mode: Mode, answers: UserAnswers): Call

  def minimum: BigDecimal = BigDecimal(0)
  def maximum: BigDecimal = BigDecimal("999999999.99")
  def maximumErrorKey: String = s"$messagePrefix.error.tooHigh"
  def messageArgs(@unused answers: UserAnswers): Seq[String] = Seq.empty
}
