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
import play.api.libs.json.{JsError, JsString, Json}

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
            mandatoryFields = AlfMandatoryFields()
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
      (json \ "options" \ "selectPageConfig" \ "showNoneOfTheseOption").as[Boolean] mustBe false
      (json \ "options" \ "manualAddressEntryConfig" \ "mandatoryFields" \ "town").as[Boolean] mustBe true
      (json \ "options" \ "manualAddressEntryConfig" \ "line1MaxLength").as[Int] mustBe 35
      (json \ "labels" \ "en" \ "countryPickerLabels" \ "heading").as[String] mustBe "Country heading"
      (json \ "labels" \ "en" \ "international" \ "editPageLabels" \ "townLabel").as[String] mustBe "Town"
      json.as[AlfJourneyConfig] mustBe config
    }

    "must fail to read invalid app labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("navTitle" -> 1),
        Json.obj("navTitle" -> "title", "phaseBannerHtml" -> 1),
        Json.obj("navTitle" -> 1, "phaseBannerHtml" -> 1)
      ).foreach { json =>
        json.validate[AlfAppLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid confirm page labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("title" -> 1),
        Json.obj("title" -> "title", "heading" -> "heading", "submitLabel" -> 1),
        Json.obj("title" -> 1, "heading" -> 1, "submitLabel" -> 1)
      ).foreach { json =>
        json.validate[AlfConfirmPageLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid country picker labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("title" -> 1),
        Json.obj("title" -> "title", "heading" -> "heading", "countryLabel" -> "label", "submitLabel" -> 1),
        Json.obj("title" -> 1, "heading" -> 1, "countryLabel" -> 1, "submitLabel" -> 1)
      ).foreach { json =>
        json.validate[AlfCountryPickerLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid edit page labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("title" -> 1),
        Json.obj(
          "title" -> "title",
          "heading" -> "heading",
          "line1Label" -> "line1",
          "line2Label" -> "line2",
          "line3Label" -> "line3",
          "townLabel" -> "town",
          "postcodeLabel" -> "postcode",
          "countryLabel" -> "country",
          "submitLabel" -> 1
        ),
        Json.obj("title" -> 1, "heading" -> 1, "line1Label" -> 1, "submitLabel" -> 1)
      ).foreach { json =>
        json.validate[AlfEditPageLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid international labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("editPageLabels" -> "text"),
        Json.obj("editPageLabels" -> Json.obj())
      ).foreach { json =>
        json.validate[AlfInternationalLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid journey config" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("options" -> "text"),
        Json.obj("options" -> Json.obj(), "labels" -> Json.obj()),
        Json.obj("options" -> "text", "labels" -> "text")
      ).foreach { json =>
        json.validate[AlfJourneyConfig] mustBe a[JsError]
      }
    }

    "must fail to read invalid labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("appLevelLabels" -> "text"),
        Json.obj(
          "appLevelLabels" -> Json.obj("navTitle" -> "title", "phaseBannerHtml" -> "banner"),
          "international" -> "text"
        )
      ).foreach { json =>
        json.validate[AlfLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid labels config" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("en" -> "text"),
        Json.obj("en" -> Json.obj())
      ).foreach { json =>
        json.validate[AlfLabelsConfig] mustBe a[JsError]
      }
    }

    "must fail to read invalid lookup page labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("title" -> 1),
        Json.obj(
          "title" -> "title",
          "heading" -> "heading",
          "filterLabel" -> "filter",
          "postcodeLabel" -> "postcode",
          "submitLabel" -> 1
        ),
        Json.obj("title" -> 1, "heading" -> 1, "filterLabel" -> 1, "postcodeLabel" -> 1, "submitLabel" -> 1)
      ).foreach { json =>
        json.validate[AlfLookupPageLabels] mustBe a[JsError]
      }
    }

    "must fail to read invalid mandatory fields" in {
      Seq(
        JsString("text"),
        Json.obj("addressLine1" -> "text"),
        Json.obj(
          "addressLine1" -> true,
          "addressLine2" -> false,
          "addressLine3" -> false,
          "town" -> true,
          "postcode" -> "text"
        ),
        Json.obj(
          "addressLine1" -> "text",
          "addressLine2" -> "text",
          "addressLine3" -> "text",
          "town" -> "text",
          "postcode" -> "text"
        )
      ).foreach { json =>
        json.validate[AlfMandatoryFields] mustBe a[JsError]
      }
    }

    "must read mandatory fields with defaults" in {
      Json.obj().as[AlfMandatoryFields] mustBe AlfMandatoryFields()
    }

    "must fail to read invalid manual address entry config" in {
      val mandatoryFields = Json.obj(
        "addressLine1" -> true,
        "addressLine2" -> false,
        "addressLine3" -> false,
        "town" -> true,
        "postcode" -> false
      )
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("mandatoryFields" -> "text"),
        Json.obj("mandatoryFields" -> mandatoryFields, "line1MaxLength" -> "text"),
        Json.obj("mandatoryFields" -> "text", "showOrganisationName" -> "text")
      ).foreach { json =>
        json.validate[AlfManualAddressEntryConfig] mustBe a[JsError]
      }
    }

    "must read manual address entry config with defaults" in {
      Json.obj("mandatoryFields" -> Json.obj()).as[AlfManualAddressEntryConfig] mustBe
        AlfManualAddressEntryConfig(AlfMandatoryFields())
    }

    "must fail to read invalid options" in {
      val options = Json.obj(
        "continueUrl" -> "/continue",
        "signOutHref" -> "/sign-out",
        "deskProServiceName" -> "service",
        "phaseFeedbackLink" -> "/feedback",
        "manualAddressEntryConfig" -> Json.obj(
          "mandatoryFields" -> Json.obj(
            "addressLine1" -> true,
            "addressLine2" -> false,
            "addressLine3" -> false,
            "town" -> true,
            "postcode" -> false
          )
        )
      )
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("continueUrl" -> 1),
        options + ("selectPageConfig" -> JsString("text")),
        options + ("ukMode" -> JsString("text")),
        Json.obj("continueUrl" -> 1, "signOutHref" -> 1, "manualAddressEntryConfig" -> "text")
      ).foreach { json =>
        json.validate[AlfOptions] mustBe a[JsError]
      }
    }

    "must fail to read invalid select page config" in {
      Seq(
        JsString("text"),
        Json.obj("showNoneOfTheseOption" -> "text")
      ).foreach { json =>
        json.validate[AlfSelectPageConfig] mustBe a[JsError]
      }
    }

    "must fail to read invalid select page labels" in {
      Seq(
        JsString("text"),
        Json.obj(),
        Json.obj("title" -> 1),
        Json.obj(
          "title" -> "title",
          "heading" -> "heading",
          "submitLabel" -> "submit",
          "editAddressLinkText" -> 1
        ),
        Json.obj("title" -> 1, "heading" -> 1, "submitLabel" -> 1, "editAddressLinkText" -> 1)
      ).foreach { json =>
        json.validate[AlfSelectPageLabels] mustBe a[JsError]
      }
    }
  }
}
