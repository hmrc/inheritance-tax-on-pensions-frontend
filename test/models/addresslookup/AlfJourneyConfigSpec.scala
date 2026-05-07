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

import base.SpecBase
import play.api.libs.json.Json

class AlfJourneyConfigSpec extends SpecBase {

  "AlfJourneyConfig" - {

    "must read and write json" in {

      val editPageLabels = AlfEditPageLabels(
        title = "Edit title",
        heading = "Edit heading",
        line1Label = "Line 1",
        line2Label = "Line 2",
        line3Label = "Line 3",
        townLabel = "Town",
        postcodeLabel = "Postcode",
        countryLabel = "Country",
        submitLabel = "Continue"
      )

      val config = AlfJourneyConfig(
        options = AlfOptions(
          continueUrl = "/continue",
          signOutHref = "/sign-out",
          deskProServiceName = "inheritance-tax-on-pensions-frontend",
          phaseFeedbackLink = "/feedback",
          allowedCountryCodes = Some(Seq("GB", "FR")),
          ukMode = true,
          manualAddressEntryConfig = AlfManualAddressEntryConfig(
            mandatoryFields = AlfMandatoryFields(
              addressLine1 = true,
              addressLine2 = true,
              addressLine3 = false,
              town = true,
              postcode = false
            )
          )
        ),
        labels = AlfLabelsConfig(
          AlfLabels(
            appLevelLabels = AlfAppLabels("Service name", "Phase banner"),
            countryPickerLabels =
              AlfCountryPickerLabels("Country title", "Country heading", "Country label", "Continue"),
            selectPageLabels = AlfSelectPageLabels("Select title", "Select heading", "Continue", "Edit address"),
            lookupPageLabels = AlfLookupPageLabels("Lookup title", "Lookup heading", "Filter", "Postcode", "Continue"),
            confirmPageLabels = AlfConfirmPageLabels("Confirm title", "Confirm heading", "Use this address"),
            editPageLabels = editPageLabels,
            international = AlfInternationalLabels(editPageLabels)
          )
        )
      )

      val json = Json.toJson(config)

      (json \ "version").as[Int] mustBe 2
      (json \ "options" \ "continueUrl").as[String] mustBe "/continue"
      (json \ "options" \ "allowedCountryCodes").as[Seq[String]] mustBe Seq("GB", "FR")
      (json \ "options" \ "manualAddressEntryConfig" \ "mandatoryFields" \ "town").as[Boolean] mustBe true
      (json \ "labels" \ "en" \ "countryPickerLabels" \ "heading").as[String] mustBe "Country heading"
      (json \ "labels" \ "en" \ "international" \ "editPageLabels" \ "townLabel").as[String] mustBe "Town"
      json.as[AlfJourneyConfig] mustBe config
    }
  }
}
