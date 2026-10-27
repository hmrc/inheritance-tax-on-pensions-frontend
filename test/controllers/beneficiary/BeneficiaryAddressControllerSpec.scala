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

package controllers.beneficiary

import play.api.test.FakeRequest
import services.{AddressLookupFrontendService, UserAnswersService}
import pages.AreBeneficiariesKnownPage
import play.api.inject.bind
import models.addresslookup.{AlfAddress, AlfAddressData, AlfCountry}
import base.SpecBase
import models._
import pages.beneficiary._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import models.beneficiary.BeneficiaryType
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class BeneficiaryAddressControllerSpec extends SpecBase {
  private def answers(role: JourneyRole): UserAnswers = (for {
    first <- individualBeneficiaryUserAnswers.set(BeneficiaryAddressPage(0), testPrAddress)
    typed <- first.set(
      BeneficiaryTypePage(1),
      role match {
        case JourneyRole.BeneficiaryTrust => BeneficiaryType.Trust
        case _ => BeneficiaryType.Individual
      }
    )
    contact <- typed.set(BeneficiaryNamePage(1, role), individualName)
    named <-
      if (role == JourneyRole.BeneficiaryTrust) contact.set(BeneficiaryTrustNamePage(1), trustName)
      else contact.set(BeneficiaryHasNinoPage(1), true).flatMap(_.set(BeneficiaryNinoPage(1), testNino))
    address <- named.set(BeneficiaryAddressPage(1), testPrAddress)
  } yield address).success.value

  private val internationalAddress = AlfAddressData(
    Some("address-id"),
    AlfAddress(None, Seq("2 ABCDE Street"), None, Some("75001"), AlfCountry("FR", "France"))
  )

  Seq(JourneyRole.BeneficiaryIndividual, JourneyRole.BeneficiaryTrust).foreach { role =>
    Seq(NormalMode, CheckMode).foreach { mode =>
      s"start address lookup for the selected $role in $mode" in {
        val service = mock[AddressLookupFrontendService]
        when(service.initBeneficiaryJourney(any(), any(), any(), any(), any())(using any(), any()))
          .thenReturn(Future.successful("/lookup-address"))
        val app = applicationBuilder(Some(answers(role)), usesSession = true)
          .overrides(bind[AddressLookupFrontendService].toInstance(service))
          .build()
        running(app) {
          val result = route(app, FakeRequest(GET, routes.BeneficiaryAddressController.start(srn, 1, mode).url)).value
          redirectLocation(result).value mustBe "/lookup-address"
          val name = if (role == JourneyRole.BeneficiaryTrust) trustName else individualNameFormatted
          verify(service).initBeneficiaryJourney(eqTo(srn), eqTo(1), eqTo(mode), eqTo(name), eqTo(role))(using
            any(),
            any()
          )
        }
      }

    }

    s"replace only the selected $role address when changing from UK to international" in {
      val original = answers(role).set(BeneficiaryIhtPayablePage(1), BigDecimal("12.34")).success.value
      val expected = original
        .set(BeneficiaryAddressPage(1), PrAddress.fromAlfAddressData(internationalAddress))
        .success
        .value
      val service = mock[AddressLookupFrontendService]
      val userAnswersService = mock[UserAnswersService]
      when(service.getAddress(eqTo("address-id"))(using any())).thenReturn(Future.successful(internationalAddress))
      when(userAnswersService.set(any())(using any(), any())).thenReturn(Future.successful(Right(expected)))
      val app = applicationBuilder(Some(original), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(service),
          bind[UserAnswersService].toInstance(userAnswersService)
        )
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, CheckMode, "address-id").url)
        ).value
        status(result) mustBe SEE_OTHER
        val captured = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(userAnswersService).set(captured.capture())(using any(), any())
        captured.getValue mustBe expected
        captured.getValue.get(BeneficiaryAddressPage(1)).value.postCode mustBe None
      }
    }

    s"continue to the $role IHT amount page after address lookup in NormalMode" in {
      val original = answers(role).set(BeneficiaryIhtPayablePage(1), BigDecimal("12.34")).success.value
      val service = mock[AddressLookupFrontendService]
      val userAnswersService = mock[UserAnswersService]
      when(service.getAddress(any())(using any())).thenReturn(Future.successful(internationalAddress))
      when(userAnswersService.set(any())(using any(), any())).thenReturn(Future.successful(Right(original)))
      val app = applicationBuilder(Some(original), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(service),
          bind[UserAnswersService].toInstance(userAnswersService)
        )
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, NormalMode, "address-id").url)
        ).value
        redirectLocation(result).value mustBe routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, NormalMode).url
      }
    }

    s"return to CYA after changing the $role address when its IHT amount is already saved" in {
      val original = answers(role).set(BeneficiaryIhtPayablePage(1), BigDecimal("12.34")).success.value
      val service = mock[AddressLookupFrontendService]
      val userAnswersService = mock[UserAnswersService]
      when(service.getAddress(any())(using any())).thenReturn(Future.successful(internationalAddress))
      when(userAnswersService.set(any())(using any(), any())).thenReturn(Future.successful(Right(original)))
      val app = applicationBuilder(Some(original), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(service),
          bind[UserAnswersService].toInstance(userAnswersService)
        )
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, CheckMode, "address-id").url)
        ).value
        redirectLocation(result).value mustBe controllers.routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }
  }

  "recover for missing callback ids, invalid beneficiaries and excluded beneficiary journeys" in {
    val service = mock[AddressLookupFrontendService]
    val data = answers(JourneyRole.BeneficiaryIndividual)
    Seq(
      data -> routes.BeneficiaryAddressController.continue(srn, 1, NormalMode).url,
      data -> routes.BeneficiaryAddressController.start(srn, 2, NormalMode).url,
      data.set(AreBeneficiariesKnownPage, false).success.value -> routes.BeneficiaryAddressController
        .start(srn, 1, NormalMode)
        .url,
      data.remove(BeneficiaryNinoPage(1)).success.value -> routes.BeneficiaryAddressController
        .start(srn, 1, NormalMode)
        .url,
      data
        .remove(BeneficiaryNamePage(1, JourneyRole.BeneficiaryIndividual))
        .success
        .value -> routes.BeneficiaryAddressController
        .start(srn, 1, NormalMode)
        .url
    ).foreach { (userAnswers, url) =>
      val app = applicationBuilder(Some(userAnswers), usesSession = true)
        .overrides(bind[AddressLookupFrontendService].toInstance(service))
        .build()
      running(app) {
        val result = route(app, FakeRequest(GET, url)).value
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
    verifyNoInteractions(service)
  }

  "recover without saving when ALF returns an empty address" in {
    val service = mock[AddressLookupFrontendService]
    val userAnswersService = mock[UserAnswersService]
    val returned = internationalAddress.copy(address = internationalAddress.address.copy(lines = Seq.empty))
    when(service.getAddress(any())(using any())).thenReturn(Future.successful(returned))
    val app = applicationBuilder(Some(answers(JourneyRole.BeneficiaryTrust)), usesSession = true)
      .overrides(
        bind[AddressLookupFrontendService].toInstance(service),
        bind[UserAnswersService].toInstance(userAnswersService)
      )
      .build()
    running(app) {
      val result = route(
        app,
        FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, CheckMode, "address-id").url)
      ).value
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      verifyNoInteractions(userAnswersService)
    }
  }

  "recover when saving the beneficiary address fails" in {
    val service = mock[AddressLookupFrontendService]
    val userAnswersService = mock[UserAnswersService]
    when(service.getAddress(any())(using any())).thenReturn(Future.successful(internationalAddress))
    when(userAnswersService.set(any())(using any(), any()))
      .thenReturn(Future.successful(Left(UpstreamErrorResponse("save failed", 500))))
    val app = applicationBuilder(Some(answers(JourneyRole.BeneficiaryTrust)), usesSession = true)
      .overrides(
        bind[AddressLookupFrontendService].toInstance(service),
        bind[UserAnswersService].toInstance(userAnswersService)
      )
      .build()
    running(app) {
      val result = route(
        app,
        FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, CheckMode, "address-id").url)
      ).value
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "collect a missing amount after changing the beneficiary address" in {
    val original = answers(JourneyRole.BeneficiaryTrust)
    val saved =
      original.set(BeneficiaryAddressPage(1), PrAddress.fromAlfAddressData(internationalAddress)).success.value
    val service = mock[AddressLookupFrontendService]
    val userAnswersService = mock[UserAnswersService]
    when(service.getAddress(any())(using any())).thenReturn(Future.successful(internationalAddress))
    when(userAnswersService.set(any())(using any(), any())).thenReturn(Future.successful(Right(saved)))
    val app = applicationBuilder(Some(original), usesSession = true)
      .overrides(
        bind[AddressLookupFrontendService].toInstance(service),
        bind[UserAnswersService].toInstance(userAnswersService)
      )
      .build()
    running(app) {
      val result = route(
        app,
        FakeRequest(GET, routes.BeneficiaryAddressController.continue(srn, 1, CheckMode, "address-id").url)
      ).value
      redirectLocation(result).value mustBe routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, CheckMode).url
    }
  }
}
