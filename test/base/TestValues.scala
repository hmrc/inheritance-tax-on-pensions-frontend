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
import play.api.libs.json.Json
import models._

import java.time.LocalDate

trait TestValues extends Generators {
  val srn: SchemeId.Srn = srnGen.sample.get
  val userAnswersId = "id"
  val schemeName = "testSchemeName"
  val email = "testEmail@test.com"
  val inheritanceTaxReference = "A123456/25A"
  val paymentReference = "A123456/25A629671"
  val testDateOfBirth: LocalDate = LocalDate.of(1950, 1, 1)
  val testDateOfDeath: LocalDate = LocalDate.of(2020, 1, 1)
  val testPaymentNoticeDate: LocalDate = LocalDate.of(2026, 2, 2)
  val testIndex: Int = 0
  val testInvalidBeneficiaryIndexes: List[Int] = List(-1, 30)
  val testUuid = "test-uuid"

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    schemeName = schemeName,
    pstr = "testPSTR",
    schemeStatus = SchemeStatus.Open,
    schemeType = "testSchemeType",
    authorisingPSAID = Some("A1234567"),
    establishers = List(Establisher(SensitiveString("testFirstName testLastName"), EstablisherKind.Individual))
  )

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")

  val defaultMinimalDetails: MinimalDetails = MinimalDetails(
    SensitiveString(email),
    isPsaSuspended = false,
    None,
    Some(
      SensitiveIndividualDetails(
        SensitiveString(individualDetails.firstName),
        individualDetails.middleName.map(SensitiveString(_)),
        SensitiveString(individualDetails.lastName)
      )
    ),
    rlsFlag = false,
    deceasedFlag = false
  )
  val individualName: IndividualName = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Lastname"
  )
  val organisationName = "Testdata Company Ltd"
  val trustName = "Testdata Trust"
  val testPrAddress: PrAddress =
    PrAddress("1 ABCDE Street", None, None, Some("FGHIJ Town"), Some("AA1 1AA"), "GB")
  val individualNameFormatted: String = s"${individualName.firstForename} ${individualName.surname}"

  def deceasedPrUserAnswers: UserAnswers =
    UserAnswers(userAnswersId, srnGen.sample.get.value.toString, testUuid)
      .copy(
        data = Json.obj(
          "inheritanceTaxReference" -> "F123456/25A",
          "nameOfDeceased" -> Json.obj(
            "firstForename" -> "dec",
            "surname" -> "name"
          ),
          "hasNino" -> false,
          "reasonForNoNino" -> "no nino",
          "birthDeathDates" -> Json.obj(
            "dateOfBirth" -> "1920-01-01",
            "dateOfDeath" -> "2026-01-01"
          ),
          "didPrSubmit" -> true,
          "ihtTaxInformation" -> Json.obj(
            "dateThePensionSchemeReceivedNoticeToPay" -> "2026-01-01"
          ),
          "areBeneficiariesKnown" -> true
        )
      )

  val prOrganisationUserAnswers: UserAnswers =
    deceasedPrUserAnswers
      .copy(
        data = deceasedPrUserAnswers.data ++ Json.obj(
          "prType" -> "organisation",
          "prDetails" -> Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> "AB Org",
              "title" -> "Mr",
              "firstForename" -> "Firstname",
              "secondForename" -> "Middlename",
              "surname" -> "Surname",
              "addressline1" -> "33 AB Street",
              "addressline2" -> "AB Area",
              "addressline3" -> "Some District",
              "addressline4" -> "Anytown",
              "ukPostcode" -> "ZZ1 1ZZ",
              "country" -> "GB"
            )
          )
        )
      )

  val prOrganisationUserAnswersNoAddress: UserAnswers =
    deceasedPrUserAnswers
      .copy(
        data = deceasedPrUserAnswers.data ++ Json.obj(
          "prType" -> "organisation",
          "prDetails" -> Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> "AB Org",
              "title" -> "Mr",
              "firstForename" -> "Firstname",
              "secondForename" -> "Middlename",
              "surname" -> "Surname"
            )
          )
        )
      )

  val prIndividualUserAnswers: UserAnswers =
    deceasedPrUserAnswers
      .copy(
        data = deceasedPrUserAnswers.data ++ Json.obj(
          "prType" -> "individual",
          "prDetails" -> Json.obj(
            "individual" -> Json.obj(
              "title" -> "Ms",
              "firstForename" -> "Firstnametwo",
              "secondForename" -> "Middlenametwo",
              "surname" -> "Surname",
              "addressline1" -> "33 Fake Street",
              "addressline2" -> "AB Area",
              "addressline3" -> "Some District",
              "addressline4" -> "Anytown",
              "ukPostcode" -> "ZZ1 1ZZ",
              "country" -> "GB"
            )
          )
        )
      )

  val prIndividualUserAnswersNoAddress: UserAnswers =
    deceasedPrUserAnswers
      .copy(
        data = deceasedPrUserAnswers.data ++ Json.obj(
          "prType" -> "individual",
          "prDetails" -> Json.obj(
            "individual" -> Json.obj(
              "title" -> "Ms",
              "firstForename" -> "Firstnametwo",
              "secondForename" -> "Middlenametwo",
              "surname" -> "Surname"
            )
          )
        )
      )

}
