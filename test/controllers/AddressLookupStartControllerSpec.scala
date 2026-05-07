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
import services.AddressLookupFrontendService
import pages.IndividualNamePage
import play.api.inject.bind
import base.SpecBase
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class AddressLookupStartControllerSpec extends SpecBase {

  "AddressLookupStartController" - {

    "must start the ALF journey and redirect to the returned ALF URL" in {

      val lprIndividualName = IndividualName(Some("Mr"), "John", Some("William"), "Doe")
      val userAnswers =
        emptyUserAnswers.set(IndividualNamePage(JourneyRole.LprIndividual), lprIndividualName).success.value
      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

      when(
        mockAddressLookupFrontendService.initJourney(eqTo(srn), eqTo(NormalMode), eqTo("John Doe"))(using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful("/lookup-address"))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupStartController.start(srn, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/lookup-address"
        verify(mockAddressLookupFrontendService).initJourney(eqTo(srn), eqTo(NormalMode), eqTo("John Doe"))(using
          any(),
          any()
        )
      }
    }

    "must start a new ALF journey in CheckMode" in {

      val lprIndividualName = IndividualName(Some("Mr"), "John", Some("William"), "Doe")
      val userAnswers =
        emptyUserAnswers.set(IndividualNamePage(JourneyRole.LprIndividual), lprIndividualName).success.value
      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

      when(
        mockAddressLookupFrontendService.initJourney(eqTo(srn), eqTo(CheckMode), eqTo("John Doe"))(using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful("/lookup-address"))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupStartController.start(srn, CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/lookup-address"
        verify(mockAddressLookupFrontendService).initJourney(eqTo(srn), eqTo(CheckMode), eqTo("John Doe"))(using
          any(),
          any()
        )
      }
    }

    "must redirect to journey recovery when the LPR individual name is missing" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddressLookupStartController.start(srn, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAddressLookupFrontendService, never).initJourney(any(), any(), any())(using any(), any())
      }
    }
  }
}
