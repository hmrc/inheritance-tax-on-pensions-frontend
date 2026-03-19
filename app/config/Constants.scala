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

package config

object Constants {

  val psaEnrolmentKey = "HMRC-PODS-ORG"
  val pspEnrolmentKey = "HMRC-PODSPP-ORG"

  val psaIdKey = "PSAID"
  val pspIdKey = "PSPID"

  val PSA = "PSA"
  val PSP = "PSP"

  val SRN = "SRN"

  val delimitedPSA = "DELIMITED_PSAID"
  val detailsNotFound = "no match found"

  val PREPOPULATION_FLAG = "prePopulationFlag"

  // iHTP auth headers
  val SRN_HEADER = "srn"
  val USERNAME_HEADER = "userName"
  val SCHEME_NAME_HEADER = "schemeName"
  val ROLE_HEADER = "requestRole"

  object Css {

    val bodyCssClass = "govuk-body"
    val hintCssClass = "govuk-hint"

    val headingXLCssClass = "govuk-heading-xl"
    val headingLCssClass = "govuk-heading-l"
    val headingMCssClass = "govuk-heading-m"

    val labelCssLargeClass = "govuk-label--l"

    val numberListCssClass = "govuk-list govuk-list--number"
    val bulletListCssClass = "govuk-list govuk-list--bullet"
    val noBulletListCssClass = "govuk-list"
    val dashedListItemCssClass = "dashed-list-item"

    val visuallyHiddenCssClass = "govuk-visually-hidden"

    val captionXLCssClass = "govuk-caption-xl"
    val captionLCssClass = "govuk-caption-l"

    val formGroupCssClass = "govuk-form-group"
    val formGroupErrorCssClass = "govuk-form-group--error"

    val linkNoVisitedStateCssClass = "govuk-link govuk-link--no-visited-state"

    val jsVisibleCssClass = "hmrc-!-js-visible"
    val displayNonePrintCssClass = "govuk-!-display-none-print"

    val sectionBreakCssClass = "govuk-section-break"
    val sectionBreakLCssClass = "govuk-section-break--l"
    val sectionBreakVisibleCssClass = "govuk-section-break--visible"
  }
}
