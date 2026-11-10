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

import config.FrontendAppConfig
import play.api.Logging
import repositories.UserAnswersLockRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationLong

import javax.inject.Inject

class LockService @Inject() (
  userAnswersLockRepository: UserAnswersLockRepository,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def takeLock(id: String, owner: String): Future[Boolean] = {
    logger.info(s"[LockService] - takeLock id=$id")
    userAnswersLockRepository.takeLock(id, owner, appConfig.lockTtl.seconds).map(_.isDefined)
  }

  def acquireOrRenew(id: String, owner: String): Future[Boolean] =
    takeLock(id, owner).flatMap {
      case true => Future.successful(true)
      case false => renewLock(id, owner)
    }

  def renewLock(id: String, owner: String): Future[Boolean] =
    userAnswersLockRepository.refreshExpiry(id, owner, appConfig.lockTtl.seconds)

  def isLocked(id: String, owner: String): Future[Boolean] =
    userAnswersLockRepository.isLocked(id, owner)

  def releaseLock(id: String, owner: String): Future[Unit] = {
    logger.info(s"[LockService] - releaseLock id=$id")
    userAnswersLockRepository.releaseLock(id, owner)
  }
}
