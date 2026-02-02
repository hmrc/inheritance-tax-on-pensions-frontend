/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import play.api.test.FakeRequest
import org.mockito.Mockito._
import base.SpecBase
import models.PensionSchemeId.{PsaId, PspId}
import repositories.SessionRepository
import models.UserAnswers
import models.requests.{IdentifierRequest, OptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(sessionRepository: SessionRepository) extends DataRetrievalActionImpl(sessionRepository) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {
      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.get("id")).thenReturn(Future(None))
      val action = new Harness(sessionRepository)

      "and identified as PSA" - {
        "must set userAnswers to 'None' in the request" in {

          val identifierRequest =
            administratorRequestGen(FakeRequest()).map(_.copy(userId = "id", psaId = PsaId("A1234567"))).sample.value
          val result = action.callTransform(identifierRequest).futureValue

          result.userAnswers must not be defined
        }
      }
      "and identified as PSP" - {
        "must set userAnswers to 'None' in the request" in {

          val identifierRequest =
            practitionerRequestGen(FakeRequest()).map(_.copy(userId = "id", pspId = PspId("A1234567"))).sample.value
          val result = action.callTransform(identifierRequest).futureValue

          result.userAnswers must not be defined
        }
      }
    }

    "when there is data in the cache" - {

      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.get("id")).thenReturn(Future(Some(UserAnswers("id"))))
      val action = new Harness(sessionRepository)

      "and identified as PSA" - {
        "must build a userAnswers object and add it to the request" in {

          val identifierRequest =
            administratorRequestGen(FakeRequest()).map(_.copy(userId = "id", psaId = PsaId("A1234567"))).sample.value
          val result = action.callTransform(identifierRequest).futureValue

          result.userAnswers mustBe defined
        }
      }

      "and identified as PSP" - {
        "must build a userAnswers object and add it to the request" in {

          val identifierRequest =
            practitionerRequestGen(FakeRequest()).map(_.copy(userId = "id", pspId = PspId("A1234567"))).sample.value
          val result = action.callTransform(identifierRequest).futureValue

          result.userAnswers mustBe defined
        }
      }

    }
  }
}
