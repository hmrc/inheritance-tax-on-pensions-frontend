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

class IhtpReportSubmissionResponseSpec extends SpecBase {

  "IhtpReportSubmissionResponse" - {

    "must successfully read from json" in {
      val json = Json.obj(
        "formBundleNo" -> "bundle-1",
        "ihtPaymentReference" -> "payment-1"
      )

      val result = json.as[IhtpReportSubmissionResponse]
      result.formBundleNo mustBe "bundle-1"
      result.ihtPaymentReference mustBe "payment-1"
    }

    "must successfully write to json" in {
      val response = IhtpReportSubmissionResponse(
        formBundleNo = "bundle-1",
        ihtPaymentReference = "payment-1"
      )

      val json = Json.toJson(response)
      (json \ "formBundleNo").as[String] mustBe "bundle-1"
      (json \ "ihtPaymentReference").as[String] mustBe "payment-1"
    }
  }
}
