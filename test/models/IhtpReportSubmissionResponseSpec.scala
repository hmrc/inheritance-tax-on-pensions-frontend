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

import base.SpecBase
import play.api.libs.json.Json

import java.time.Instant

class IhtpReportSubmissionResponseSpec extends SpecBase {

  "IhtpReportSubmissionResponse" - {

    "must successfully read from json" in {
      val json = Json.obj(
        "processingDateTime" -> "2026-01-01T00:00:00Z",
        "formBundleNumber" -> "bundle-1",
        "paymentReference" -> "payment-1"
      )

      val result = json.as[IhtpReportSubmissionResponse]
      result.processingDateTime mustBe Instant.parse("2026-01-01T00:00:00Z")
      result.formBundleNumber mustBe "bundle-1"
      result.paymentReference mustBe "payment-1"
    }

    "must successfully write to json" in {
      val response = IhtpReportSubmissionResponse(
        processingDateTime = Instant.parse("2026-01-01T00:00:00Z"),
        formBundleNumber = "bundle-1",
        paymentReference = "payment-1"
      )

      val json = Json.toJson(response)
      (json \ "processingDateTime").as[String] mustBe "2026-01-01T00:00:00Z"
      (json \ "formBundleNumber").as[String] mustBe "bundle-1"
      (json \ "paymentReference").as[String] mustBe "payment-1"
    }

    "must have implicit formats defined" in {
      import IhtpReportSubmissionResponse.formats
      val json = Json.obj(
        "processingDateTime" -> "2026-01-01T00:00:00Z",
        "formBundleNumber" -> "bundle-1",
        "paymentReference" -> "payment-1"
      )
      val result = json.as[IhtpReportSubmissionResponse]
      result mustBe IhtpReportSubmissionResponse(
        processingDateTime = Instant.parse("2026-01-01T00:00:00Z"),
        formBundleNumber = "bundle-1",
        paymentReference = "payment-1"
      )
    }
  }
}
