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

package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.WhatYouWillNeedView
import base.SpecBase
import models.NormalMode

class WhatYouWillNeedControllerSpec extends SpecBase {

  lazy val whatYouWillNeedRoute: String = routes.WhatYouWillNeedController.onPageLoad(srn).url

  "WhatYouWillNeed Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, whatYouWillNeedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatYouWillNeedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(srn)(using request, messages(application)).toString
      }
    }

    // TODO - repurpose InputPagePlaceholderController to the next input page within the minimal journey
    "must redirect to InputPagePlaceholderController list when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, whatYouWillNeedRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InputPagePlaceholderController.onPageLoad(srn, NormalMode).url
      }
    }
  }
}
