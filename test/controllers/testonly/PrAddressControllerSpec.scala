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
import pages.PrTypePage
import play.api.inject.bind
import models.SchemeId.Srn
import base.SpecBase
import controllers.actions.AllowAccessActionWithSessionCacheProvider
import play.api.libs.json.{JsObject, Json}
import models.{PrAddress, PrType, UserAnswers}
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future, Promise}

class PrAddressControllerSpec extends SpecBase {

  private val url = s"/inheritance-tax-on-pensions/test-only/${srn.value}/seed-pr-address"
  private val originalDetails = Json.toJsObject(individualName) ++ Json.obj("organisationName" -> organisationName)

  private def answers(prType: PrType): UserAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "prType" -> prType.toString,
      "prDetails" -> Json.obj(prType.toString -> originalDetails),
      "unrelatedAnswer" -> "unchanged"
    )
  )

  private def builder(userAnswers: Option[UserAnswers], service: UserAnswersService, isPsa: Boolean = true) =
    applicationBuilder(userAnswers = userAnswers, usesSession = true, isPsa = isPsa)
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .overrides(
        bind[UserAnswersService].toInstance(service),
        bind[SessionSchemeDetailsRepository].toInstance(mock[SessionSchemeDetailsRepository]),
        bind[SessionMinimalDetailsRepository].toInstance(mock[SessionMinimalDetailsRepository])
      )

  "PrAddressController" - {
    for {
      prType <- PrType.values
      isPsa <- Seq(true, false)
    }
      s"must save a $prType address for isPsa=$isPsa, preserving the report without contacting ALF" in {
        val service = mock[UserAnswersService]
        val alf = mock[AddressLookupFrontendService]
        val initial = answers(prType)
        when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(initial)))
        val application = builder(Some(initial), service, isPsa)
          .overrides(bind[AddressLookupFrontendService].toInstance(alf))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, url)).value
          status(result) mustBe OK
          contentAsString(result) mustBe "Address created"

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).set(captor.capture())(using any(), any())
          val saved = captor.getValue
          saved.id mustBe initial.id
          saved.uuid mustBe initial.uuid
          saved.srn mustBe initial.srn
          (saved.data \ "unrelatedAnswer").as[String] mustBe "unchanged"
          saved.get(PrTypePage) mustBe Some(prType)
          val details = (saved.data \ "prDetails" \ prType.toString).as[JsObject]
          details.as[PrAddress] mustBe testPrAddress
          details mustBe (originalDetails ++ Json.toJsObject(testPrAddress))
          verifyNoInteractions(alf)
        }
      }

    "must replace stale address fields and support repeated calls" in {
      val initial = answers(PrType.Individual)
      val withOldAddress = initial.copy(data =
        initial.data.deepMerge(
          Json.obj(
            "prDetails" -> Json.obj(
              "individual" -> Json.obj(
                "addressline1" -> "Old address",
                "addressline2" -> "Old second line",
                "addressline3" -> "Old third line",
                "country" -> "FR"
              )
            )
          )
        )
      )
      val expected = initial.copy(data =
        initial.data.deepMerge(
          Json.obj(
            "prDetails" -> Json.obj("individual" -> Json.toJsObject(testPrAddress))
          )
        )
      )

      Seq(withOldAddress, expected).foreach { existing =>
        val service = mock[UserAnswersService]
        when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(expected)))
        val application = builder(Some(existing), service).build()
        running(application) {
          val result = route(application, FakeRequest(GET, url)).value
          status(result) mustBe OK
          contentAsString(result) mustBe "Address created"
          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).set(captor.capture())(using any(), any())
          captor.getValue mustBe expected
        }
      }
    }

    Seq(
      "missing PR type" -> emptyUserAnswers,
      "missing PR details" -> emptyUserAnswers.copy(data = Json.obj("prType" -> "individual")),
      "missing report UUID" -> answers(PrType.Individual).copy(uuid = "")
    ).foreach { (description, existing) =>
      s"must reject $description without saving" in {
        val service = mock[UserAnswersService]
        val application = builder(Some(existing), service).build()
        running(application) {
          status(route(application, FakeRequest(GET, url)).value) mustBe BAD_REQUEST
          verify(service, never).set(any())(using any(), any())
        }
      }
    }

    "must use journey recovery when user answers cannot be retrieved" in {
      val service = mock[UserAnswersService]
      val application = builder(None, service).build()
      running(application) {
        val result = route(application, FakeRequest(GET, url)).value
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
        verifyNoInteractions(service)
      }
    }

    "must wait for persistence and report an unsuccessful save" in {
      val service = mock[UserAnswersService]
      val save = Promise[Either[UpstreamErrorResponse, UserAnswers]]()
      when(service.set(any())(using any(), any())).thenReturn(save.future)
      val application = builder(Some(answers(PrType.Individual)), service).build()
      running(application) {
        val result = route(application, FakeRequest(GET, url)).value
        result.isCompleted mustBe false
        save.success(Left(UpstreamErrorResponse("Failed to save", INTERNAL_SERVER_ERROR)))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "must not expose the endpoint through the production router" in {
      val service = mock[UserAnswersService]
      val application = builder(Some(answers(PrType.Individual)), service)
        .configure("play.http.router" -> "prod.Routes")
        .build()
      running(application) {
        status(route(application, FakeRequest(GET, url)).value) mustBe NOT_FOUND
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
            ): Future[Result] =
              Future.successful(Results.Forbidden)
          }
      }
      val application = applicationBuilder(userAnswers = Some(answers(PrType.Individual)))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(
          bind[UserAnswersService].toInstance(service),
          bind[SessionSchemeDetailsRepository].toInstance(mock[SessionSchemeDetailsRepository]),
          bind[SessionMinimalDetailsRepository].toInstance(mock[SessionMinimalDetailsRepository]),
          bind[AllowAccessActionWithSessionCacheProvider].toInstance(deniedAccess)
        )
        .build()
      running(application) {
        status(route(application, FakeRequest(GET, url)).value) mustBe FORBIDDEN
        verifyNoInteractions(service)
      }
    }
  }
}
