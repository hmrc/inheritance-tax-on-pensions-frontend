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

package forms.mappings

import scala.util.matching

trait Regex {
  // This allows European characters
  val europeanCharacterRange: String = "a-zA-Z\u00C0-\u00FF\u0100-\u024F\u0370-\u03FF\u0400-\u04FF"

  // Fallback if above nameRegex can't be used, allows West European (latin) characters only:
  val westEuropeanCharacterRange: String = "A-Za-zÀ-ÖØ-öø-ÿ"

  val addressLineRegex: String = s"^[${europeanCharacterRange}0-9 \\-,.'\\/#:;º@_\\[\\]\\?\\(\\)\\&]+$$"

  val ukPostcodeEtmpsRegularExpr: String = s"^([A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,3})$$"

  val reasonForNoNinoRegex: String = """^[a-zA-Z0-9\- \t,./()]+$"""

  val nameRegex: String = s"^[${europeanCharacterRange}]+(?:[ '-][${europeanCharacterRange}]+)*$$"

  val ihtReferenceNumberRegex: String = "^[AF]\\d{6}/\\d{2}[A-Z]$"

  val schemeAdminIdRegex: matching.Regex = "^(A[0-9]{7})$".r

  val orgAndTrustNameRegex: String = s"^[${westEuropeanCharacterRange}0-9 \\-,.'\\/@_\\[\\]\\(\\)\\&]+$$"

}
