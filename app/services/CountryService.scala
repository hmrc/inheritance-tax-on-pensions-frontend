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

import models.address.Country
import play.api.Environment
import play.api.libs.json.Json

import scala.io.Source
import scala.util.Using

import javax.inject.{Inject, Singleton}

@Singleton
class CountryService @Inject() (environment: Environment) {

  private val countriesJsonPath = "countries.json"

  lazy val countries: Seq[Country] = loadCountries()

  def findByCode(code: String): Option[Country] =
    countries.find(_.code == code)

  def nameForCode(code: String): String =
    findByCode(code).map(_.name).getOrElse(code)

  private def loadCountries(): Seq[Country] =
    environment
      .resourceAsStream(countriesJsonPath)
      .flatMap { stream =>
        Using(stream) { stream =>
          Json.parse(Source.fromInputStream(stream).mkString)
        }.toOption
      }
      .fold(throw new RuntimeException("Couldn't load country data")) { json =>
        json
          .validate[Seq[Country]]
          .fold(
            errors => throw new RuntimeException(s"Failed to parse countries JSON: $errors"),
            countries => countries.sortBy(_.name)
          )
      }
}
