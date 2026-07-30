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

package helpers

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustEqual
import play.api.libs.json._

trait FormatHelper extends AnyFreeSpec {

  def testFormat[Model](model: Model)(using Reads[Model], OWrites[Model]): Unit = {
    val modelName = model.getClass.getSimpleName
    s"$modelName should" - {
      "convert to JSON and back again without modifying any field names" in {
        val json = Json.toJson(model)
        json.as[Model] mustEqual model
      }
    }
  }
}
