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
import base.SpecBase
import models.{SchemeDetails, SessionSchemeDetails}
import org.mockito.ArgumentMatchers._
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, Future}

import java.time.Instant

class SessionServiceSpec extends SpecBase {

  override def beforeEach(): Unit = {
    reset(mockSessionSchemeDetailsRepository,
      mockSessionMinimalDetailsRepository)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val schemeDetailsCache: SchemeDetails =
    schemeDetailsGen.sample.value.copy(schemeName = "cache")

  val schemeDetailsApi: SchemeDetails =
    schemeDetailsGen.sample.value.copy(schemeName = "api")

  private val sessionSchemeDetails: SessionSchemeDetails =
    SessionSchemeDetails("id", "srn", schemeDetailsCache, Instant.ofEpochSecond(1))

  val mockSessionSchemeDetailsRepository: SessionSchemeDetailsRepository = mock[SessionSchemeDetailsRepository]
  val mockSessionMinimalDetailsRepository: SessionMinimalDetailsRepository = mock[SessionMinimalDetailsRepository]

  val sessionService = SessionService(mockSessionSchemeDetailsRepository, mockSessionMinimalDetailsRepository)

  "trySchemeDetails" - {

    "return scheme details from the session when session data is present" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(Some(sessionSchemeDetails)))
      val result = Await.result(sessionService.trySchemeDetails("id", "srn", Future.successful(Some(schemeDetailsApi))), patienceConfig.timeout)
      result mustBe Some(schemeDetailsCache)
    }

    "return scheme details from the api when session data is not present" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(None))
      val result = Await.result(sessionService.trySchemeDetails("id", "srn", Future.successful(Some(schemeDetailsApi))), patienceConfig.timeout)
      result mustBe Some(schemeDetailsApi)
      verify(mockSessionSchemeDetailsRepository, times(1)).set(any())
    }

    "return none when the scheme details are not present in the session or the api" in {
      when(mockSessionSchemeDetailsRepository.get(any())).thenReturn(Future.successful(None))
      val result = Await.result(sessionService.trySchemeDetails("id", "srn", Future.successful(None)), patienceConfig.timeout)
      result mustBe None
      verify(mockSessionSchemeDetailsRepository, times(0)).set(any())
    }
  }

  "tryMinimalDetails" - {

  }
}
