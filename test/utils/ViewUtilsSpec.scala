/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import org.scalatest.matchers.must.Matchers
import forms.mappings.Mappings
import org.scalatest.OptionValues
import views.ViewUtils
import play.api.i18n.Messages
import play.api.data.Form
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec

class ViewUtilsSpec extends AnyFreeSpec with OptionValues with Matchers with Mappings {
  val validTestForm: Form[String] =
    Form(
      "value" -> text(),
    )

  val validErrorForm: Form[String] =
    Form(
      "value" -> text(),
    ).bind(Map("value" -> ""))

  implicit val messages: Messages = stubMessages()

  ".title" - {

    "valid form must not start with space" in {
      val result = ViewUtils.title(validTestForm, "testTitle", None)(using messages)
      result must not startWith " testTitle"
    }
    "form with error should be prefixed and space delimited" in {
      val result = ViewUtils.title(validErrorForm, "testTitle", None)(using messages)
      result mustEqual "error.title.prefix testTitle - service.name - site.govuk"
    }
  }
}
