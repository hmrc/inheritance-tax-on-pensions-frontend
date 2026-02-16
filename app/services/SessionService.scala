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

import com.google.inject.Inject
import connectors.MinimalDetailsError
import play.api.Logging
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import models._

import scala.concurrent.{ExecutionContext, Future}

class SessionService @Inject() (
  sessionSchemeDetailsRepository: SessionSchemeDetailsRepository,
  sessionMinimalDetailsRepository: SessionMinimalDetailsRepository
) extends Logging {

  def trySchemeDetails(
    id: String,
    srn: String,
    callBackFunction: => Future[Option[SchemeDetails]]
  )(implicit ec: ExecutionContext): Future[Option[SchemeDetails]] =
    sessionSchemeDetailsRepository.get(id).flatMap {
      case Some(sessionSchemeDetails) =>
        if (sessionSchemeDetails.srn != srn) {
          logger.warn(s"[SessionService] - The SRN provided does not match that of the cached session authorisation")
          throw new IllegalArgumentException("The SRN provided does not match that of the cached session authorisation")
        }
        Future.successful(Some(sessionSchemeDetails.schemeDetails))
      case None =>
        callBackFunction.map {
          case Some(schemeDetails) =>
            sessionSchemeDetailsRepository.set(SessionSchemeDetails(id, srn, schemeDetails))
            Some(schemeDetails)
          case _ => None
        }
    }

  def tryMinimalDetails(
    id: String,
    srn: String,
    callBackFunction: => Future[Either[MinimalDetailsError, MinimalDetails]]
  )(implicit ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    sessionMinimalDetailsRepository.get(id).flatMap {
      case Some(sessionMinimalDetails) =>
        if (sessionMinimalDetails.srn != srn) {
          logger.warn(s"[SessionService] - The SRN provided does not match that of the cached session authorisation")
          throw new IllegalArgumentException("The SRN provided does not match that of the cached session authorisation")
        }
        Future.successful(Right(sessionMinimalDetails.minimalDetails))
      case None =>
        callBackFunction.map {
          case Right(minimalDetails) =>
            sessionMinimalDetailsRepository.set(SessionMinimalDetails(id, srn, minimalDetails))
            Right(minimalDetails)
          case Left(error) => Left(error)
        }
    }

  def clearSession(id: String): Future[Boolean] = {
    sessionSchemeDetailsRepository.clear(id)
    sessionMinimalDetailsRepository.clear(id)
  }

  def cacheAllowAccessDetails(
    sessionSchemeDetails: SessionSchemeDetails,
    sessionMinimalDetails: SessionMinimalDetails
  ): Future[Boolean] = {
    sessionSchemeDetailsRepository.set(sessionSchemeDetails)
    sessionMinimalDetailsRepository.set(sessionMinimalDetails)
  }

}
