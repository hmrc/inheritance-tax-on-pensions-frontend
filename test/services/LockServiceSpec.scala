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

import org.mockito.Mockito.{never, verify, when}
import config.FrontendAppConfig
import base.SpecBase
import repositories.UserAnswersLockRepository
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

import java.time.Instant

class LockServiceSpec extends SpecBase {

  private val lockId = s"${srn.value}-$testUuid"
  private val owner = "owner-id"
  private val ttl = 900L

  private def fixture(): (LockService, UserAnswersLockRepository) = {
    val repository = mock[UserAnswersLockRepository]
    val appConfig = mock[FrontendAppConfig]
    when(appConfig.lockTtl).thenReturn(ttl)
    (new LockService(repository, appConfig), repository)
  }

  "takeLock" - {

    "must return true when the lock is acquired" in {
      val (service, repository) = fixture()
      when(repository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(Instant.now())))

      service.takeLock(lockId, owner).futureValue mustBe true
      verify(repository).takeLock(eqTo(lockId), eqTo(owner), eqTo(ttl.seconds))
    }

    "must return false when the lock is already held" in {
      val (service, repository) = fixture()
      when(repository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))

      service.takeLock(lockId, owner).futureValue mustBe false
    }
  }

  "acquireOrRenew" - {

    "must return true and not renew when the lock is acquired" in {
      val (service, repository) = fixture()
      when(repository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(Instant.now())))

      service.acquireOrRenew(lockId, owner).futureValue mustBe true
      verify(repository, never()).refreshExpiry(any(), any(), any())
    }

    "must return true when the lock is held by the same owner (renewed)" in {
      val (service, repository) = fixture()
      when(repository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))
      when(repository.refreshExpiry(any(), any(), any())).thenReturn(Future.successful(true))

      service.acquireOrRenew(lockId, owner).futureValue mustBe true
      verify(repository).refreshExpiry(eqTo(lockId), eqTo(owner), eqTo(ttl.seconds))
    }

    "must return false when the lock is held by another owner" in {
      val (service, repository) = fixture()
      when(repository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))
      when(repository.refreshExpiry(any(), any(), any())).thenReturn(Future.successful(false))

      service.acquireOrRenew(lockId, owner).futureValue mustBe false
    }
  }

  "renewLock" - {

    "must refresh the lock expiry with the configured ttl" in {
      val (service, repository) = fixture()
      when(repository.refreshExpiry(any(), any(), any())).thenReturn(Future.successful(true))

      service.renewLock(lockId, owner).futureValue mustBe true
      verify(repository).refreshExpiry(eqTo(lockId), eqTo(owner), eqTo(ttl.seconds))
    }
  }

  "isLocked" - {

    "must delegate to the repository" in {
      val (service, repository) = fixture()
      when(repository.isLocked(any(), any())).thenReturn(Future.successful(true))

      service.isLocked(lockId, owner).futureValue mustBe true
      verify(repository).isLocked(eqTo(lockId), eqTo(owner))
    }
  }

  "releaseLock" - {

    "must delegate to the repository" in {
      val (service, repository) = fixture()
      when(repository.releaseLock(any(), any())).thenReturn(Future.successful(()))

      service.releaseLock(lockId, owner).futureValue mustBe ()
      verify(repository).releaseLock(eqTo(lockId), eqTo(owner))
    }
  }
}
