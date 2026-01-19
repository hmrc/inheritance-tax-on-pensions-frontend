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

package generators

import play.api.mvc.Request
import models.PensionSchemeId.{PsaId, PspId}
import org.scalacheck.Gen
import models.PensionSchemeId
import models.requests.IdentifierRequest
import org.scalacheck.Gen._
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}

trait ModelGenerators {
  def nonEmptyString: Gen[String] =
    for {
      c <- alphaNumChar
      s <- alphaNumStr
    } yield s"$c$s"

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId.apply)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId.apply)
  val pensionSchemeIdGen: Gen[PensionSchemeId] = Gen.oneOf(psaIdGen, pspIdGen)

  def practitionerRequestGen[A](request: Request[A]): Gen[PractitionerRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      pspId <- pspIdGen
    } yield PractitionerRequest(userId, externalId, request, pspId)

  def administratorRequestGen[A](request: Request[A]): Gen[AdministratorRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      psaId <- psaIdGen
    } yield AdministratorRequest(userId, externalId, request, psaId)

  def identifierRequestGen[A](request: Request[A]): Gen[IdentifierRequest[A]] =
    Gen.oneOf(administratorRequestGen[A](request), practitionerRequestGen[A](request))
}

object ModelGenerators extends ModelGenerators
