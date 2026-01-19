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

package generators

import org.scalacheck.Gen._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.{Instant, LocalDate, ZoneOffset}

trait BasicGenerators {
  def genIntersperseString(gen: Gen[String], value: String, frequencyV: Int = 1, frequencyN: Int = 10): Gen[String] = {

    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield seq1.toSeq.zip(seq2).foldLeft("") {
      case (acc, (n, Some(v))) =>
        acc + n + v
      case (acc, (n, _)) =>
        acc + n
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt].suchThat(x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt].suchThat(x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr.suchThat(_.size > 0)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map("%f".format(_))

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int].suchThat(_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int].suchThat(_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int].suchThat(x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    for {
      c <- alphaNumChar
      s <- alphaNumStr
    } yield s"$c$s"

  def nonEmptyAlphaString: Gen[String] =
    for {
      c <- alphaChar
      s <- alphaStr
    } yield s"$c$s"

  val uniqueStringGen: Gen[String] = Gen.uuid.map(_.toString)

//  def nonEmptyListOf[A](gen: Gen[A]): Gen[NonEmptyList[A]] =
//    Gen.nonEmptyListOf(gen).map(list => NonEmptyList(list.head, list.tail))

  def stringLengthBetween(minLength: Int, maxLength: Int, charGen: Gen[Char]): Gen[String] =
    for {
      length <- choose(minLength, maxLength)
      chars <- listOfN(length, charGen)
    } yield chars.mkString

  def numericStringLength(length: Int): Gen[String] =
    stringLengthBetween(length, length, numChar)

  def numericStringLengthBetween(minLength: Int, maxLength: Int): Gen[String] =
    stringLengthBetween(minLength, maxLength, numChar)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- (minLength * 2).max(100)
    length <- Gen.chooseNum(minLength + 1, maxLength)
    chars <- listOfN(length, arbitrary[Char])
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString.suchThat(!excluded.contains(_))

  def stringContains(value: String): Gen[String] =
    for {
      s <- nonEmptyString
      i <- chooseNum(0, s.length)
      (l, r) = s.splitAt(i)
    } yield s"$l$value$r"

  val boolean: Gen[Boolean] =
    Gen.oneOf(true, false)

  val topLevelDomain: Gen[String] =
    Gen.oneOf("com", "gov.uk", "co.uk", "net", "org", "io")

  val emailGen: Gen[String] =
    for {
      username <- nonEmptyString
      domain <- nonEmptyString
      topDomain <- topLevelDomain
    } yield s"$username@$domain.$topDomain"

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  val earliestDate: LocalDate = LocalDate.of(1970, 1, 1)
  val latestDate: LocalDate = LocalDate.of(3000, 12, 31)

  def date: Gen[LocalDate] =
    datesBetween(earliestDate, latestDate)

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }
}
