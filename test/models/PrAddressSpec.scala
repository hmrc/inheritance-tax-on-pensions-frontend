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

import models.addresslookup.{AlfAddress, AlfAddressData, AlfCountry}
import base.SpecBase

class PrAddressSpec extends SpecBase {

  "fromAlfAddressData" - {

    "must map ALF town to address line 4 when address line 2 is present" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street", "Fake Area", "Fake County"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "33 Fake Street",
        addressLine2 = Some("Fake Area"),
        addressLine3 = Some("Fake County"),
        addressLine4 = Some("Fakeville"),
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must map ALF town to address line 2 when address line 2 is missing" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "33 Fake Street",
        addressLine2 = Some("Fakeville"),
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must prepend the PO Box and shift existing address lines down" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street", "Fake Area"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = Some("999")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "PO Box 999",
        addressLine2 = Some("33 Fake Street"),
        addressLine3 = Some("Fake Area"),
        addressLine4 = Some("Fakeville"),
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must not duplicate the PO Box when it is already in the address lines" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("PO Box 999", "Fake Area"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = Some("999")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "PO Box 999",
        addressLine2 = Some("Fake Area"),
        addressLine3 = None,
        addressLine4 = Some("Fakeville"),
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must use the PO Box as address line 1 when no address lines are returned" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq.empty,
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = Some("PO Box 999")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "PO Box 999",
        addressLine2 = Some("Fakeville"),
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must not duplicate ALF town when it is used to populate address line 2" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("PO Box 999"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "PO Box 999",
        addressLine2 = Some("Fakeville"),
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }

    "must not duplicate ALF town when it is already returned as the last address line" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street", "Fakeville"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.fromAlfAddressData(addressData) mustBe PrAddress(
        addressLine1 = "33 Fake Street",
        addressLine2 = Some("Fakeville"),
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = Some("ZZ1 1ZZ"),
        country = "GB"
      )
    }
  }

  "hasValidFirstAddressLine" - {

    "must return true when ALF address line 1 is present" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.hasValidFirstAddressLine(addressData) mustBe true
    }

    "must return true when ALF address lines are empty but a PO Box is present" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq.empty,
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = Some("999")
        )
      )

      PrAddress.hasValidFirstAddressLine(addressData) mustBe true
    }

    "must return false when ALF address line 1 is blank" in {

      val addressData = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("  "),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      PrAddress.hasValidFirstAddressLine(addressData) mustBe false
    }
  }
}
