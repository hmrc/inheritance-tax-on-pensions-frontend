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
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import models._

import scala.concurrent.{ExecutionContext, Future}

class SessionService @Inject() (
  sessionSchemeDetailsRepository: SessionSchemeDetailsRepository,
  sessionMinimalDetailsRepository: SessionMinimalDetailsRepository
) {

  def trySchemeDetails(
    id: String,
    srn: String,
    callBackFunction: => Future[Option[SchemeDetails]]
  )(implicit ec: ExecutionContext): Future[Option[SchemeDetails]] =
    sessionSchemeDetailsRepository.get(id).flatMap {
      case Some(sessionSchemeDetails) =>
        // If the cached srn does not match the current then invalidate the cache and call the api sequentially
        if (sessionSchemeDetails.srn != srn) {
          for {
            _ <- sessionSchemeDetailsRepository.clear(id)
            maybeSchemeDetails <- schemeDetailsApiCall(id, srn, callBackFunction)
          } yield maybeSchemeDetails
        } else {
          Future.successful(Some(sessionSchemeDetails.schemeDetails))
        }
      case None =>
        schemeDetailsApiCall(id, srn, callBackFunction)
    }

  def tryMinimalDetails(
    id: String,
    srn: String,
    callBackFunction: => Future[Either[MinimalDetailsError, MinimalDetails]]
  )(implicit ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    sessionMinimalDetailsRepository.get(id).flatMap {
      case Some(sessionMinimalDetails) =>
        // If the cached srn does not match the current then invalidate the cache and call the api sequentially
        if (sessionMinimalDetails.srn != srn) {
          for {
            _ <- sessionMinimalDetailsRepository.clear(id)
            maybeMinimalDetails <- minimalDetailsApiCall(id, srn, callBackFunction)
          } yield maybeMinimalDetails
        } else {
          Future.successful(Right(sessionMinimalDetails.minimalDetails))
        }
      case None =>
        minimalDetailsApiCall(id, srn, callBackFunction)
    }

  private def schemeDetailsApiCall(
    id: String,
    srn: String,
    callBackFunction: => Future[Option[SchemeDetails]]
  )(implicit ec: ExecutionContext): Future[Option[SchemeDetails]] =
    callBackFunction.map {
      case Some(schemeDetails) =>
        sessionSchemeDetailsRepository.set(SessionSchemeDetails(id, srn, schemeDetails))
        Some(schemeDetails)
      case _ => None
    }

  private def minimalDetailsApiCall(
    id: String,
    srn: String,
    callBackFunction: => Future[Either[MinimalDetailsError, MinimalDetails]]
  )(implicit ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    callBackFunction.map {
      case Right(minimalDetails) =>
        sessionMinimalDetailsRepository.set(SessionMinimalDetails(id, srn, minimalDetails))
        Right(minimalDetails)
      case Left(error) => Left(error)
    }
}
