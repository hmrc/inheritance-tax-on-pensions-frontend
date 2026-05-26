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

package controllers

import play.api.test.FakeRequest
import services.{AddressLookupFrontendService, UserAnswersService}
import play.api.inject.bind
import models.addresslookup.{AlfAddress, AlfAddressData, AlfCountry}
import base.SpecBase
import play.api.libs.json.{JsObject, Json}
import models.{LprAddress, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class AddressLookupContinueControllerSpec extends SpecBase {

  private val addressId = "address-id"
  private val validAddressData = AlfAddressData(
    id = Some(addressId),
    address = AlfAddress(
      organisation = None,
      lines = Seq("33 Fake Street", "Fake Area"),
      town = Some("Fakeville"),
      postcode = Some("ZZ1 1ZZ"),
      country = AlfCountry("GB", "United Kingdom")
    )
  )

  "AddressLookupContinueController" - {

    "must save the selected address and redirect to the next page when ALF returns a valid address" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(validAddressData))
      when(mockUserAnswersService.set(any())(using any(), any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupContinueController.continue(srn, NormalMode, addressId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersService).set(userAnswersCaptor.capture())(using any(), any())

        (userAnswersCaptor.getValue.data \ "lprDetails" \ "individual").as[LprAddress] mustBe
          LprAddress(
            addressLine1 = "33 Fake Street",
            addressLine2 = Some("Fake Area"),
            addressLine3 = None,
            addressLine4 = Some("Fakeville"),
            ukPostcode = Some("ZZ1 1ZZ"),
            country = "GB"
          )
      }
    }

    "must replace stale address fields when changing to an address with fewer lines" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      val existingUserAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "lprDetails" -> Json.obj(
            "individual" -> Json.obj(
              "firstForename" -> "John",
              "surname" -> "Doe",
              "addressLine1" -> "33 Fake Street",
              "addressLine2" -> "Fake Area",
              "addressLine3" -> "Some District",
              "addressLine4" -> "Anytown",
              "ukPostcode" -> "ZZ1 1ZZ",
              "country" -> "GB"
            )
          )
        )
      )

      val newAddressData = AlfAddressData(
        id = Some(addressId),
        address = AlfAddress(
          organisation = None,
          lines = Seq("11 A Boulevard", "Fakeville"),
          town = Some("Fakeville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(newAddressData))
      when(mockUserAnswersService.set(any())(using any(), any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(existingUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupContinueController.continue(srn, NormalMode, addressId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersService).set(userAnswersCaptor.capture())(using any(), any())

        val updatedIndividual = (userAnswersCaptor.getValue.data \ "lprDetails" \ "individual").as[JsObject]

        updatedIndividual mustBe Json.obj(
          "firstForename" -> "John",
          "surname" -> "Doe",
          "addressLine1" -> "11 A Boulevard",
          "addressLine2" -> "Fakeville",
          "ukPostcode" -> "ZZ1 1ZZ",
          "country" -> "GB"
        )
      }
    }

    "must redirect to journey recovery when no address id is returned from ALF" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupContinueController.continue(srn, NormalMode, " ").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAddressLookupFrontendService, never).getAddress(any())(using any())
        verify(mockUserAnswersService, never).set(any())(using any(), any())
      }
    }

    "must redirect to journey recovery and not save when ALF returns no address lines" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(
          Future.successful(validAddressData.copy(address = validAddressData.address.copy(lines = Seq.empty)))
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupContinueController.continue(srn, NormalMode, addressId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockUserAnswersService, never).set(any())(using any(), any())
      }
    }

    "must redirect to journey recovery and not save when ALF returns a blank first address line" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(validAddressData.copy(address = validAddressData.address.copy(lines = Seq(" ")))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupContinueController.continue(srn, NormalMode, addressId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockUserAnswersService, never).set(any())(using any(), any())
      }
    }
  }
}
