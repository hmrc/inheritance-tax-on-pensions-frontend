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
import services.{LockService, UserAnswersService}
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import base.SpecBase
import models.UserAnswers
import models.requests.{AllowedAccessRequest, OptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest().withSession("uuid" -> testUuid)
  ).sample.value

  val mockLockService: LockService = mock[LockService]
  when(mockLockService.renewLock(any(), any())).thenReturn(Future.successful(true))

  class Harness(userAnswersService: UserAnswersService, ls: LockService = mockLockService)
      extends DataRetrievalActionImpl(userAnswersService, ls) {
    def callTransform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {
      val notFound = mock[UpstreamErrorResponse]
      when(notFound.statusCode).thenReturn(NOT_FOUND)

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Left(notFound)))
      val action = new Harness(userAnswersService)

      "must set userAnswers to new instance in the request" in {
        val result = action.callTransform(request).futureValue
        result.userAnswers mustBe defined
      }
    }

    "when there is data in the cache" - {

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Right(UserAnswers("id", srnGen.sample.value.toString, testUuid))))
      val action = new Harness(userAnswersService)

      "must build a userAnswers object and add it to the request" in {
        val result = action.callTransform(request).futureValue

        result.userAnswers mustBe defined
      }
    }

    "when a uuid is in session" - {

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Right(UserAnswers("id", srnGen.sample.value.toString, testUuid))))
      val ls = mock[LockService]
      when(ls.renewLock(any(), any())).thenReturn(Future.successful(true))
      val action = new Harness(userAnswersService, ls)

      "must renew the report lock" in {
        action.callTransform(request).futureValue

        verify(ls).renewLock(eqTo(s"${request.srn.value}-$testUuid"), eqTo(request.getUserId))
      }
    }

    "when there is no uuid in session" - {

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Right(UserAnswers("id", srnGen.sample.value.toString, testUuid))))
      val ls = mock[LockService]
      val action = new Harness(userAnswersService, ls)
      val requestWithoutUuid: AllowedAccessRequest[AnyContentAsEmpty.type] =
        allowedAccessRequestGen(FakeRequest()).sample.value

      "must not renew a lock" in {
        action.callTransform(requestWithoutUuid).futureValue

        verify(ls, never()).renewLock(any(), any())
      }
    }

    "when the lock renewal fails" - {

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Right(UserAnswers("id", srnGen.sample.value.toString, testUuid))))
      val ls = mock[LockService]
      when(ls.renewLock(any(), any())).thenReturn(Future.failed(new RuntimeException("mongo down")))
      val action = new Harness(userAnswersService, ls)

      "must still build the request" in {
        val result = action.callTransform(request).futureValue

        result.userAnswers mustBe defined
      }
    }

    "when there is a http exception calling the back end" - {
      val serverError = mock[UpstreamErrorResponse]
      when(serverError.statusCode).thenReturn(INTERNAL_SERVER_ERROR)

      val userAnswersService: UserAnswersService = mock[UserAnswersService]
      when(userAnswersService.fetch(any())(using any(), any()))
        .thenReturn(Future(Left(serverError)))
      val action = new Harness(userAnswersService)

      "must set userAnswers to new instance in the request" in {
        val result = action.callTransform(request).futureValue
        result.userAnswers mustBe None
      }
    }
  }
}
