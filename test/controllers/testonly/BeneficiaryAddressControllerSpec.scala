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

package controllers.testonly

import play.api.test.FakeRequest
import services.{AddressLookupFrontendService, UserAnswersService}
import play.api.mvc._
import play.api.inject.bind
import models.SchemeId.Srn
import base.SpecBase
import controllers.actions.AllowAccessActionWithSessionCacheProvider
import models.UserAnswers
import pages.beneficiary.{BeneficiaryAddressPage, BeneficiaryTypePage}
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import models.beneficiary.BeneficiaryType
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future, Promise}

class BeneficiaryAddressControllerSpec extends SpecBase {

  private def url(index: Int) =
    s"/inheritance-tax-on-pensions/test-only/${srn.value}/$index/seed-beneficiary-address"

  private def builder(answers: Option[UserAnswers], service: UserAnswersService, isPsa: Boolean = true) =
    applicationBuilder(userAnswers = answers, usesSession = true, isPsa = isPsa)
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .overrides(
        bind[UserAnswersService].toInstance(service),
        bind[SessionSchemeDetailsRepository].toInstance(mock[SessionSchemeDetailsRepository]),
        bind[SessionMinimalDetailsRepository].toInstance(mock[SessionMinimalDetailsRepository])
      )

  "BeneficiaryAddressController" - {
    for {
      beneficiaryType <- BeneficiaryType.values
      isPsa <- Seq(true, false)
    }
      s"must seed only the selected $beneficiaryType address for isPsa=$isPsa without contacting ALF" in {
        val initial = individualBeneficiaryUserAnswers
          .set(BeneficiaryTypePage(1), beneficiaryType)
          .success
          .value
        val expected = initial.set(BeneficiaryAddressPage(1), testPrAddress).success.value
        val service = mock[UserAnswersService]
        val alf = mock[AddressLookupFrontendService]
        when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(expected)))
        val application = builder(Some(initial), service, isPsa)
          .overrides(bind[AddressLookupFrontendService].toInstance(alf))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, url(1))).value
          status(result) mustBe OK
          contentAsString(result) mustBe "Address created"
          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).set(captor.capture())(using any(), any())
          captor.getValue mustBe expected
          verifyNoInteractions(alf)
        }
      }

    "must replace an old address and support repeated calls" in {
      val initial = individualBeneficiaryUserAnswers
      val oldAddress = testPrAddress.copy(addressLine2 = Some("Old second line"), postCode = None, country = "FR")
      val withOldAddress = initial.set(BeneficiaryAddressPage(0), oldAddress).success.value
      val expected = initial.set(BeneficiaryAddressPage(0), testPrAddress).success.value

      Seq(withOldAddress, expected).foreach { existing =>
        val service = mock[UserAnswersService]
        when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(expected)))
        val application = builder(Some(existing), service).build()
        running(application) {
          status(route(application, FakeRequest(GET, url(0))).value) mustBe OK
          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).set(captor.capture())(using any(), any())
          captor.getValue mustBe expected
        }
      }
    }

    Seq(
      "missing beneficiary" -> emptyUserAnswers,
      "missing report UUID" -> individualBeneficiaryUserAnswers.copy(uuid = "")
    ).foreach { (description, initial) =>
      s"must reject $description without saving" in {
        val service = mock[UserAnswersService]
        val application = builder(Some(initial), service).build()
        running(application) {
          status(route(application, FakeRequest(GET, url(0))).value) mustBe BAD_REQUEST
          verifyNoInteractions(service)
        }
      }
    }

    "must not create a beneficiary at an unused index" in {
      val service = mock[UserAnswersService]
      val application = builder(Some(individualBeneficiaryUserAnswers), service).build()
      running(application) {
        status(route(application, FakeRequest(GET, url(1))).value) mustBe BAD_REQUEST
        verifyNoInteractions(service)
      }
    }

    "must reject indices outside the supported range" in {
      val service = mock[UserAnswersService]
      val application = builder(Some(individualBeneficiaryUserAnswers), service).build()
      running(application) {
        Seq(-1, 30).foreach { index =>
          status(route(application, FakeRequest(GET, url(index))).value) mustBe NOT_FOUND
        }
        verifyNoInteractions(service)
      }
    }

    "must recover when user answers are missing" in {
      val service = mock[UserAnswersService]
      val application = builder(None, service).build()
      running(application) {
        val result = route(application, FakeRequest(GET, url(0))).value
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
        verifyNoInteractions(service)
      }
    }

    "must wait for persistence and report an unsuccessful save" in {
      val service = mock[UserAnswersService]
      val save = Promise[Either[UpstreamErrorResponse, UserAnswers]]()
      when(service.set(any())(using any(), any())).thenReturn(save.future)
      val application = builder(Some(individualBeneficiaryUserAnswers), service).build()
      running(application) {
        val result = route(application, FakeRequest(GET, url(0))).value
        result.isCompleted mustBe false
        save.success(Left(UpstreamErrorResponse("Failed to save", INTERNAL_SERVER_ERROR)))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "must not expose the endpoint through the production router" in {
      val service = mock[UserAnswersService]
      val application = builder(Some(individualBeneficiaryUserAnswers), service)
        .configure("play.http.router" -> "prod.Routes")
        .build()
      running(application) {
        status(route(application, FakeRequest(GET, url(0))).value) mustBe NOT_FOUND
        verifyNoInteractions(service)
      }
    }

    "must not save when scheme access is denied" in {
      val service = mock[UserAnswersService]
      val deniedAccess = new AllowAccessActionWithSessionCacheProvider {
        override def apply(srn: Srn): ActionFunction[IdentifierRequest, AllowedAccessRequest] =
          new ActionFunction[IdentifierRequest, AllowedAccessRequest] {
            override protected def executionContext: ExecutionContext = ec
            override def invokeBlock[A](
              request: IdentifierRequest[A],
              block: AllowedAccessRequest[A] => Future[Result]
            ): Future[Result] = Future.successful(Results.Forbidden)
          }
      }
      val application = applicationBuilder(userAnswers = Some(individualBeneficiaryUserAnswers))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(
          bind[UserAnswersService].toInstance(service),
          bind[SessionSchemeDetailsRepository].toInstance(mock[SessionSchemeDetailsRepository]),
          bind[SessionMinimalDetailsRepository].toInstance(mock[SessionMinimalDetailsRepository]),
          bind[AllowAccessActionWithSessionCacheProvider].toInstance(deniedAccess)
        )
        .build()
      running(application) {
        status(route(application, FakeRequest(GET, url(0))).value) mustBe FORBIDDEN
        verifyNoInteractions(service)
      }
    }
  }
}
