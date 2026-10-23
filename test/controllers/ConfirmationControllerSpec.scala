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
import views.html.ConfirmationView
import base.SpecBase
import config.Constants._
import models.SchemeId

import java.time.format.DateTimeFormatter

class ConfirmationControllerSpec extends SpecBase {

  val testSrn: SchemeId.Srn = srn
  val url: String = "http://localhost:8204/manage-pension-schemes/pension-scheme-summary/" + testSrn.value
  val formattedDate: String = testPaymentNoticeDate.plusDays(35).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
  val needToPay: String = "10,000.00"

  "Confirmation Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad(testSrn).url)
          .withSession(PAYMENT_REFERENCE -> paymentReference)
          .withSession(FORM_BUNDLE_NO -> testFormBundleNumber)
          .withSession(DUE_DATE -> formattedDate)
          .withSession(NEED_TO_PAY -> needToPay)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          paymentReference,
          email,
          testSrn,
          url,
          formattedDate,
          schemeName,
          needToPay
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to journey recovery if one of the session variables is missing" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad(testSrn).url)
          .withSession(FORM_BUNDLE_NO -> testFormBundleNumber)
          .withSession(DUE_DATE -> formattedDate)
          .withSession(NEED_TO_PAY -> formattedDate)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmationView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
