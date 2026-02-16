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

package services

import org.mockito.Mockito._
import connectors.MinimalDetailsError
import base.SpecBase
import connectors.MinimalDetailsError.DetailsNotFound
import models._
import org.mockito.ArgumentMatchers._
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, Future}

import java.time.Instant

class SessionServiceSpec extends SpecBase {

  override def beforeEach(): Unit =
    reset(mockSessionSchemeDetailsRepository, mockSessionMinimalDetailsRepository)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val schemeDetailsCache: SchemeDetails = schemeDetailsGen.sample.value.copy(schemeName = "cache")
  val schemeDetailsApi: SchemeDetails = schemeDetailsGen.sample.value.copy(schemeName = "api")

  private val sessionSchemeDetails: SessionSchemeDetails =
    SessionSchemeDetails("id", "srn01", schemeDetailsCache, Instant.ofEpochSecond(1))

  val callbackFunctionSchemeDetails: Future[Some[SchemeDetails]] = Future.successful(Some(schemeDetailsApi))

  val detailsNotFound: MinimalDetailsError = DetailsNotFound

  val minimalDetailsCache: MinimalDetails = minimalDetailsGen.sample.value.copy(
    rlsFlag = false,
    deceasedFlag = false,
    organisationName = Some("cache")
  )
  val minimalDetailsApi: MinimalDetails = minimalDetailsGen.sample.value.copy(
    rlsFlag = false,
    deceasedFlag = false,
    organisationName = Some("api")
  )

  private val sessionMinimalDetails: SessionMinimalDetails =
    SessionMinimalDetails("id", "srn01", minimalDetailsCache, Instant.ofEpochSecond(1))

  val callbackFunctionMinimalDetails: Future[Right[Nothing, MinimalDetails]] =
    Future.successful(Right(minimalDetailsApi))

  val mockSessionSchemeDetailsRepository: SessionSchemeDetailsRepository = mock[SessionSchemeDetailsRepository]
  val mockSessionMinimalDetailsRepository: SessionMinimalDetailsRepository = mock[SessionMinimalDetailsRepository]

  val sessionService = SessionService(mockSessionSchemeDetailsRepository, mockSessionMinimalDetailsRepository)

  "trySchemeDetails" - {

    "return scheme details from the session when session data is present" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(Some(sessionSchemeDetails)))

      val result = Await.result(
        sessionService.trySchemeDetails("id", "srn01", callbackFunctionSchemeDetails),
        patienceConfig.timeout
      )

      result mustBe Some(schemeDetailsCache)
    }

    "return scheme details from the api when session data is not present" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(None))

      val result = Await.result(
        sessionService.trySchemeDetails("id", "srn01", callbackFunctionSchemeDetails),
        patienceConfig.timeout
      )

      result mustBe Some(schemeDetailsApi)
      verify(mockSessionSchemeDetailsRepository, times(0)).clear(any())
      verify(mockSessionSchemeDetailsRepository, times(1)).set(any())
    }

    "return none when the scheme details are not present in the session or the api" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(None))

      val result =
        Await.result(sessionService.trySchemeDetails("id", "srn01", Future.successful(None)), patienceConfig.timeout)

      result mustBe None
      verify(mockSessionSchemeDetailsRepository, times(0)).clear(any())
      verify(mockSessionSchemeDetailsRepository, times(0)).set(any())
    }

    "throw an exception when the session srn differs from the request srn" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(Some(sessionSchemeDetails)))

      val result = intercept[IllegalArgumentException] {
        Await.result(
          sessionService.trySchemeDetails("id", "srn02", callbackFunctionSchemeDetails),
          patienceConfig.timeout
        )
      }

      result.isInstanceOf[IllegalArgumentException] mustBe true
      result.getMessage mustBe "The SRN provided does not match that of the cached session authorisation"
    }
  }

  "tryMinimalDetails" - {

    "return minimal details from the session when session data is present" in {
      when(mockSessionMinimalDetailsRepository.get(any())).thenReturn(Future.successful(Some(sessionMinimalDetails)))

      val result = Await.result(
        sessionService.tryMinimalDetails("id", "srn01", callbackFunctionMinimalDetails),
        patienceConfig.timeout
      )

      result mustBe Right(minimalDetailsCache)
    }

    "return minimal details from the api when session data is not present" in {
      when(mockSessionMinimalDetailsRepository.get(any())).thenReturn(Future.successful(None))

      val result = Await.result(
        sessionService.tryMinimalDetails("id", "srn01", callbackFunctionMinimalDetails),
        patienceConfig.timeout
      )

      result mustBe Right(minimalDetailsApi)
      verify(mockSessionMinimalDetailsRepository, times(1)).set(any())
    }

    "return an error when the minimal details are not present in the session or the api" in {
      when(mockSessionMinimalDetailsRepository.get(any())).thenReturn(Future.successful(None))

      val result =
        Await.result(
          sessionService.tryMinimalDetails("id", "srn01", Future.successful(Left(detailsNotFound))),
          patienceConfig.timeout
        )

      result mustBe Left(detailsNotFound)
      verify(mockSessionMinimalDetailsRepository, times(0)).set(any())
    }

    "throw an exception when the session srn differs from the request srn" in {
      when(mockSessionMinimalDetailsRepository.get(any())).thenReturn(Future.successful(Some(sessionMinimalDetails)))

      val result = intercept[IllegalArgumentException] {
        Await.result(
          sessionService.tryMinimalDetails("id", "srn02", callbackFunctionMinimalDetails),
          patienceConfig.timeout
        )
      }

      result.isInstanceOf[IllegalArgumentException] mustBe true
      result.getMessage mustBe "The SRN provided does not match that of the cached session authorisation"
    }
  }

  "clearSession" - {

    "clears the session when called" in {
      when(mockSessionSchemeDetailsRepository.clear(any())).thenReturn(Future.successful(true))
      when(mockSessionMinimalDetailsRepository.clear(any())).thenReturn(Future.successful(true))

      val result = Await.result(
        sessionService.clearSession("id"),
        patienceConfig.timeout
      )

      result mustBe true
      verify(mockSessionSchemeDetailsRepository, times(1)).clear(any())
      verify(mockSessionSchemeDetailsRepository, times(1)).clear(any())
    }

  }
}
