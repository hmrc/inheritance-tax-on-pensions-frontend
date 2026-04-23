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
import pages.{InheritanceTaxReferencePage, NameOfDeceasedPage, NinoOrReasonPage}
import views.html.CheckYourAnswersView
import base.SpecBase
import viewmodels.govuk.all.SummaryListViewModel
import forms.NinoOrReasonFormData
import models.{NameOfDeceased, NinoOrReason}
import viewmodels.CheckAnswers._

class CheckYourAnswersControllerSpec extends SpecBase {

  "CheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, "A123456/25A")
        .success
        .value
        .set(
          NameOfDeceasedPage,
          NameOfDeceased(
            title = Some("Mr"),
            firstForename = "John",
            secondForename = Some("William"),
            surname = "Doe"
          )
        )
        .success
        .value
        .set(
          NinoOrReasonPage,
          NinoOrReasonFormData(
            NinoOrReason.Yes,
            Some("QQ123456C"),
            None
          )
        )
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(srn).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val summaryList = SummaryListViewModel(
          rows = Seq(
            InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
            NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
            NinoOrReasonSummary.row(srn, userAnswers)(using messages(application)).get
          )
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(srn, summaryList)(using request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(srn).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the psa confirmation page on post for psa" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PsaDeclarationController.onPageLoad(srn).url
      }
    }

    "must redirect to the psp confirmation page on post for psp" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), isPsa = false).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PspDeclarationController.onPageLoad(srn).url
      }
    }
  }
}
