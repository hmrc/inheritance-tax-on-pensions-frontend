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

package models.addresslookup

import play.api.libs.json.{Json, OFormat}

final case class AlfJourneyConfig(version: Int = 2, options: AlfOptions, labels: AlfLabelsConfig)

object AlfJourneyConfig {
  implicit val format: OFormat[AlfJourneyConfig] = Json.format[AlfJourneyConfig]
}

final case class AlfOptions(
  continueUrl: String,
  signOutHref: String,
  useNewGovUkServiceNavigation: Boolean = true,
  deskProServiceName: String,
  phaseFeedbackLink: String,
  showPhaseBanner: Boolean = true,
  alphaPhase: Boolean = true,
  disableTranslations: Boolean = true,
  includeHMRCBranding: Boolean = false,
  allowedCountryCodes: Option[Seq[String]] = None,
  ukMode: Boolean = false,
  manualAddressEntryConfig: AlfManualAddressEntryConfig,
  pageHeadingStyle: String = "govuk-heading-l"
)

object AlfOptions {
  implicit val format: OFormat[AlfOptions] = Json.format[AlfOptions]
}

final case class AlfManualAddressEntryConfig(
  mandatoryFields: AlfMandatoryFields,
  showOrganisationName: Boolean = false
)

object AlfManualAddressEntryConfig {
  implicit val format: OFormat[AlfManualAddressEntryConfig] = Json.format[AlfManualAddressEntryConfig]
}

final case class AlfMandatoryFields(
  addressLine1: Boolean = true,
  addressLine2: Boolean = true,
  addressLine3: Boolean = false,
  town: Boolean = false,
  postcode: Boolean = false
)

object AlfMandatoryFields {
  implicit val format: OFormat[AlfMandatoryFields] = Json.format[AlfMandatoryFields]
}

final case class AlfLabelsConfig(en: AlfLabels)

object AlfLabelsConfig {
  implicit val format: OFormat[AlfLabelsConfig] = Json.format[AlfLabelsConfig]
}

final case class AlfLabels(
  appLevelLabels: AlfAppLabels,
  countryPickerLabels: AlfCountryPickerLabels,
  selectPageLabels: AlfSelectPageLabels,
  lookupPageLabels: AlfLookupPageLabels,
  confirmPageLabels: AlfConfirmPageLabels,
  editPageLabels: AlfEditPageLabels,
  international: AlfInternationalLabels
)

object AlfLabels {
  implicit val format: OFormat[AlfLabels] = Json.format[AlfLabels]
}

final case class AlfInternationalLabels(editPageLabels: AlfEditPageLabels)

object AlfInternationalLabels {
  implicit val format: OFormat[AlfInternationalLabels] = Json.format[AlfInternationalLabels]
}

final case class AlfAppLabels(navTitle: String, phaseBannerHtml: String)

object AlfAppLabels {
  implicit val format: OFormat[AlfAppLabels] = Json.format[AlfAppLabels]
}

final case class AlfCountryPickerLabels(title: String, heading: String, countryLabel: String, submitLabel: String)

object AlfCountryPickerLabels {
  implicit val format: OFormat[AlfCountryPickerLabels] = Json.format[AlfCountryPickerLabels]
}

final case class AlfSelectPageLabels(title: String, heading: String, submitLabel: String, editAddressLinkText: String)

object AlfSelectPageLabels {
  implicit val format: OFormat[AlfSelectPageLabels] = Json.format[AlfSelectPageLabels]
}

final case class AlfLookupPageLabels(
  title: String,
  heading: String,
  filterLabel: String,
  postcodeLabel: String,
  submitLabel: String
)

object AlfLookupPageLabels {
  implicit val format: OFormat[AlfLookupPageLabels] = Json.format[AlfLookupPageLabels]
}

final case class AlfConfirmPageLabels(title: String, heading: String, submitLabel: String)

object AlfConfirmPageLabels {
  implicit val format: OFormat[AlfConfirmPageLabels] = Json.format[AlfConfirmPageLabels]
}

final case class AlfEditPageLabels(
  title: String,
  heading: String,
  line1Label: String,
  line2Label: String,
  line3Label: String,
  townLabel: String,
  postcodeLabel: String,
  countryLabel: String,
  submitLabel: String
)

object AlfEditPageLabels {
  implicit val format: OFormat[AlfEditPageLabels] = Json.format[AlfEditPageLabels]
}
