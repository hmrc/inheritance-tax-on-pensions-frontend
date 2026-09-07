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

package forms

import play.api.data.format.Formatter
import forms.mappings.Mappings
import play.api.data.Forms.{mapping, of}
import play.api.data.validation.{Constraint, Invalid, Valid}
import models.PrAddress
import play.api.data.{Form, FormError, Mapping}

import javax.inject.Inject

class PrAddressFormProvider @Inject() extends Mappings {

  private val addresslineMaxLength = 35
  private val addresslineRegex = """^[^%$£\r\n]+$"""
  private val optionalStringFormatter: Formatter[Option[String]] = new Formatter[Option[String]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] =
      Right(data.get(key).map(_.trim).filter(_.nonEmpty))

    override def unbind(key: String, value: Option[String]): Map[String, String] =
      Map(key -> value.getOrElse(""))
  }

  def apply(country: String): Form[PrAddress] =
    Form(
      mapping(
        "addressline1" -> text("changePrAddress.error.addressline1.required")
          .transform[String](_.trim, identity)
          .verifying(
            firstError(
              nonBlank("changePrAddress.error.addressline1.required"),
              regexp(addresslineRegex, "changePrAddress.error.addressline1.invalid"),
              maxLength(addresslineMaxLength, "changePrAddress.error.addressline1.length")
            )
          ),
        "addressline2" -> optionalAddressField(
          "changePrAddress.error.addressline2.invalid",
          "changePrAddress.error.addressline2.length"
        ),
        "addressline3" -> optionalAddressField(
          "changePrAddress.error.addressline3.invalid",
          "changePrAddress.error.addressline3.length"
        ),
        "addressline4" -> optionalAddressField(
          "changePrAddress.error.addressline4.invalid",
          "changePrAddress.error.addressline4.length"
        ),
        "ukPostcode" -> optionalAddressField(
          "changePrAddress.error.ukPostcode.invalid",
          "changePrAddress.error.ukPostcode.length"
        )
      )((addressline1, addressline2, addressline3, addressline4, ukPostcode) =>
        PrAddress(addressline1, addressline2, addressline3, addressline4, ukPostcode, country)
      )(address =>
        Some(
          (
            address.addressline1,
            address.addressline2,
            address.addressline3,
            address.addressline4,
            address.ukPostcode
          )
        )
      )
    )

  private def optionalAddressField(invalidKey: String, lengthKey: String): Mapping[Option[String]] =
    of(using optionalStringFormatter)
      .verifying(
        firstError(
          optionalConstraint(regexp(addresslineRegex, invalidKey)),
          optionalConstraint(maxLength(addresslineMaxLength, lengthKey))
        )
      )

  private def optionalConstraint(constraint: Constraint[String]): Constraint[Option[String]] =
    Constraint(_.map(constraint.apply).getOrElse(Valid))

  private def nonBlank(errorKey: String): Constraint[String] =
    Constraint {
      case value if value.nonEmpty => Valid
      case _ => Invalid(errorKey)
    }
}
