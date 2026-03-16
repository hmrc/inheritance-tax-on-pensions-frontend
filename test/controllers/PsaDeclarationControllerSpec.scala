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
import views.html.PsaDeclarationView
import base.SpecBase

class PsaDeclarationControllerSpec extends SpecBase {

  lazy val psaDeclarationRoute: String = routes.PsaDeclarationController.onPageLoad(srn).url

  "PsaDeclaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, psaDeclarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PsaDeclarationView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(srn, schemeName)(
          using request,
          messages(application)
        ).toString
      }
    }

    // TODO - repurpose SubmissionListController to the next input page within the minimal journey
    "must redirect to SubmissionListController list when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, psaDeclarationRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SubmissionListController.onPageLoad(srn).url
      }
    }
  }
}
