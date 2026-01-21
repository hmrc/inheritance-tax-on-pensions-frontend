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

package base

import generators.Generators
import models._

trait TestValues extends Generators {
  val srn: SchemeId.Srn = srnGen.sample.get
  val schemeName = "testSchemeName"
  val email = "testEmail"

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    schemeName,
    "testPSTR",
    SchemeStatus.Open,
    "testSchemeType",
    Some("A1234567"),
    List(Establisher("testFirstName testLastName", EstablisherKind.Individual))
  )

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")

  val defaultMinimalDetails: MinimalDetails = MinimalDetails(
    email,
    isPsaSuspended = false,
    Some("testOrganisation"),
    Some(individualDetails),
    rlsFlag = false,
    deceasedFlag = false
  )
}
