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

package models

import utils.WithName
import play.api.libs.json.Json.JsValueWrapper
import utils.Extractors.Int
import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class SchemeDetails(
  schemeName: String,
  pstr: String,
  schemeStatus: SchemeStatus,
  schemeType: String,
  authorisingPSAID: Option[String],
  establishers: List[Establisher]
)

case class Establisher(
  name: String,
  kind: EstablisherKind
)

sealed class EstablisherKind(val value: String)

object EstablisherKind {
  case object Company extends EstablisherKind("company")
  case object Partnership extends EstablisherKind("partnership")
  case object Individual extends EstablisherKind("individual")

  implicit val reads: Reads[EstablisherKind] =
    Reads.StringReads.collect(JsonValidationError("Invalid establisher kind")) {
      case Company.value => Company
      case Partnership.value => Partnership
      case Individual.value => Individual
    }

}

object Establisher {

  private val companyEstablisherReads: Reads[Establisher] =
    (__ \ "companyDetails" \ "companyName").read[String].map(name => Establisher(name, EstablisherKind.Company))

  private val partnershipEstablisherReads: Reads[Establisher] =
    (__ \ "partnershipDetails" \ "name").read[String].map(name => Establisher(name, EstablisherKind.Partnership))

  private val individualEstablisherReads: Reads[Establisher] =
    (__ \ "establisherDetails" \ "firstName")
      .read[String]
      .and((__ \ "establisherDetails" \ "middleName").readNullable[String])
      .and((__ \ "establisherDetails" \ "lastName").read[String]) { (first, middle, last) =>
        val name = s"$first ${middle.fold("")(m => s"$m ")}$last"
        Establisher(name, EstablisherKind.Individual)
      }

  implicit val reads: Reads[Establisher] =
    (__ \ "establisherKind").read[EstablisherKind].flatMap {
      case EstablisherKind.Company => companyEstablisherReads
      case EstablisherKind.Partnership => partnershipEstablisherReads
      case EstablisherKind.Individual => individualEstablisherReads
      case unknown =>
        Reads(_ => JsError(s"Unsupported establisher kind: $unknown"))
    }

  implicit val writes: Writes[Establisher] = establisher =>
    (establisher.kind match {
      case EstablisherKind.Company =>
        Json.obj("companyDetails" -> Json.obj("companyName" -> establisher.name))
      case EstablisherKind.Partnership =>
        Json.obj("partnershipDetails" -> Json.obj("name" -> establisher.name))
      case EstablisherKind.Individual =>
        val first :: rest = establisher.name.split(" ").toList: @unchecked
        val last :: middles = rest.reverse: @unchecked
        val middle = middles.iterator.reduceOption((a, b) => s"$a $b")
        Json.obj(
          "establisherDetails" -> Json
            .obj(
              "firstName" -> first,
              "lastName" -> last
            )
            .++(middle.fold(Json.obj())(m => Json.obj("middleName" -> m)))
        )
      case _: EstablisherKind => throw new IllegalArgumentException("Unrecognised EstablisherKind value")
    }) ++ Json.obj("establisherKind" -> establisher.kind.value)
}

object SchemeDetails {

  implicit val reads: Reads[SchemeDetails] =
    (__ \ "schemeName")
      .read[String]
      .and((__ \ "pstr").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "schemeType" \ "name").read[String])
      .and((__ \ "pspDetails" \ "authorisingPSAID").readNullable[String])
      .and(
        (__ \ "establishers")
          .readWithDefault[JsArray](JsArray.empty)
          .map[List[Establisher]](l =>
            if (l.value.isEmpty) {
              Nil
            } else {
              l.as[List[Establisher]]
            }
          )
      )(SchemeDetails.apply)

  implicit val writeListEstablishers: Writes[ListEstablishers] = Json.writes[ListEstablishers]

  implicit val writes: Writes[SchemeDetails] = { details =>
    val authorisingPSAID: JsObject = details.authorisingPSAID.fold(Json.obj())(psaId =>
      Json.obj("pspDetails" -> Json.obj("authorisingPSAID" -> psaId))
    )

    Json.obj(
      "schemeName" -> details.schemeName,
      "pstr" -> details.pstr,
      "schemeStatus" -> details.schemeStatus,
      "schemeType" -> Json.obj("name" -> details.schemeType),
      "establishers" -> details.establishers
    ) ++ authorisingPSAID
  }
}

sealed trait SchemeStatus

object SchemeStatus {

  case object Pending extends WithName("Pending") with SchemeStatus
  case object PendingInfoRequired extends WithName("Pending Info Required") with SchemeStatus
  case object PendingInfoReceived extends WithName("Pending Info Received") with SchemeStatus
  case object Rejected extends WithName("Rejected") with SchemeStatus
  case object Open extends WithName("Open") with SchemeStatus
  case object Deregistered extends WithName("Deregistered") with SchemeStatus
  case object WoundUp extends WithName("Wound-up") with SchemeStatus
  case object RejectedUnderAppeal extends WithName("Rejected Under Appeal") with SchemeStatus

  implicit val reads: Reads[SchemeStatus] = {
    case JsString(Pending.name) => JsSuccess(Pending)
    case JsString(PendingInfoRequired.name) => JsSuccess(PendingInfoRequired)
    case JsString(PendingInfoReceived.name) => JsSuccess(PendingInfoReceived)
    case JsString(Rejected.name) => JsSuccess(Rejected)
    case JsString(Open.name) => JsSuccess(Open)
    case JsString(Deregistered.name) => JsSuccess(Deregistered)
    case JsString(WoundUp.name) => JsSuccess(WoundUp)
    case JsString(RejectedUnderAppeal.name) => JsSuccess(RejectedUnderAppeal)
    case _ => JsError("Unrecognized scheme status")
  }
  implicit val writes: Writes[SchemeStatus] = s => JsString(s.toString)
}

case class ListMinimalSchemeDetails(schemeDetails: List[MinimalSchemeDetails])

object ListMinimalSchemeDetails {
  implicit val reads: Reads[ListMinimalSchemeDetails] = Json.reads[ListMinimalSchemeDetails]

  implicit val writes: Writes[ListMinimalSchemeDetails] = Json.writes[ListMinimalSchemeDetails]
}

case class MinimalSchemeDetails(
  name: String,
  srn: String,
  schemeStatus: SchemeStatus,
  openDate: Option[LocalDate],
  windUpDate: Option[LocalDate]
)

object MinimalSchemeDetails {

  private val dateRegex = "(\\d{4})-(\\d{1,2})-(\\d{1,2})".r
  private implicit val readLocalDate: Reads[LocalDate] = Reads[LocalDate] {
    case JsString(dateRegex(Int(year), Int(month), Int(day))) =>
      JsSuccess(LocalDate.of(year, month, day))
    case err => JsError(s"Unable to read local date from $err")
  }

  implicit val reads: Reads[MinimalSchemeDetails] =
    (__ \ "name")
      .read[String]
      .and((__ \ "referenceNumber").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "openDate").readNullable[LocalDate])
      .and((__ \ "windUpDate").readNullable[LocalDate])(MinimalSchemeDetails.apply)

  implicit val writeMs: Writes[MinimalSchemeDetails] = { details =>
    def formatDate(date: LocalDate): String =
      date.format(DateTimeFormatter.ofPattern("yyyy-M-d"))

    val fields =
      List[Option[(String, JsValueWrapper)]](
        Some("name" -> details.name),
        Some("referenceNumber" -> details.srn),
        Some("schemeStatus" -> details.schemeStatus.toString),
        details.openDate.map(d => "openDate" -> formatDate(d)),
        details.windUpDate.map(d => "windUpDate" -> formatDate(d))
      ).flatten

    Json.obj(fields*)
  }
}

case class ListEstablishers(list: List[Establisher])
