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
import models.PensionSchemeUser.{Administrator, Practitioner}
import models.SchemeId.{Pstr, Srn}
import models.PensionSchemeId.{PsaId, PspId}
import models._
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import org.scalacheck.Gen._
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import models.SchemeStatus._
import org.scalacheck.Gen

trait ModelGenerators extends BasicGenerators {
  // identity related
  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId.apply)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId.apply)
  val pensionSchemeIdGen: Gen[PensionSchemeId] = Gen.oneOf(psaIdGen, pspIdGen)
  val pensionSchemeUserGen: Gen[PensionSchemeUser] = Gen.oneOf(Administrator, Practitioner)

  def allowedAccessRequestGen[A](request: Request[A]): Gen[AllowedAccessRequest[A]] =
    for {
      request <- identifierRequestGen[A](request)
      schemeDetails <- schemeDetailsGen
      minimalDetails <- minimalDetailsGen
      srn <- srnGen
    } yield AllowedAccessRequest(request, schemeDetails, minimalDetails, srn)

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

  // scheme related
  lazy val individualDetailsGen: Gen[IndividualDetails] =
    for {
      firstName <- nonEmptyString
      middleName <- Gen.option(nonEmptyString)
      lastName <- nonEmptyString
    } yield IndividualDetails(firstName, middleName, lastName)

  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- emailGen
      isSuspended <- boolean
      orgName <- Gen.option(nonEmptyString)
      individual <- Gen.option(individualDetailsGen)
      rlsFlag <- boolean
      deceasedFlag <- boolean
    } yield MinimalDetails(email, isSuspended, orgName, individual, rlsFlag, deceasedFlag)

  val validSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Open,
      WoundUp,
      Deregistered
    )

  val invalidSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Pending,
      PendingInfoRequired,
      PendingInfoReceived,
      Rejected,
      RejectedUnderAppeal
    )

  val schemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(validSchemeStatusGen, invalidSchemeStatusGen)

  val establisherGen: Gen[Establisher] =
    for {
      name <- Gen.listOfN(3, nonEmptyString).map(_.mkString(" "))
      kind <- Gen.oneOf(EstablisherKind.Company, EstablisherKind.Individual, EstablisherKind.Partnership)
    } yield Establisher(name, kind)

  val schemeDetailsGen: Gen[SchemeDetails] =
    for {
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      schemeType <- nonEmptyString
      authorisingPsa <- Gen.option(nonEmptyString)
      establishers <- Gen.listOfN(5, establisherGen)
    } yield SchemeDetails(name, pstr, status, schemeType, authorisingPsa, establishers)

  val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr.apply)
  val srnGen: Gen[Srn] =
    Gen
      .listOfN(10, numChar)
      .flatMap { xs =>
        Srn(s"S${xs.mkString}")
          .fold[Gen[Srn]](Gen.fail)(x => Gen.const(x))
      }
  val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)

  val minimalSchemeDetailsGen: Gen[MinimalSchemeDetails] =
    for {
      name <- nonEmptyString
      srn <- srnGen.map(_.value)
      schemeStatus <- schemeStatusGen
      openDate <- Gen.option(date)
      windUpDate <- Gen.option(date)
    } yield MinimalSchemeDetails(name, srn, schemeStatus, openDate, windUpDate)

  val listMinimalSchemeDetailsGen: Gen[ListMinimalSchemeDetails] =
    Gen.listOf(minimalSchemeDetailsGen).map(xs => ListMinimalSchemeDetails(xs))
}

object ModelGenerators extends ModelGenerators
