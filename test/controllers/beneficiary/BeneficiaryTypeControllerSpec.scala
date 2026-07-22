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

package controllers.beneficiary

import play.api.test.FakeRequest
import connectors.InheritanceTaxOnPensionsConnector
import play.api.inject.bind
import views.html.beneficiary.BeneficiaryTypeView
import base.SpecBase
import forms.beneficiary.BeneficiaryTypeFormProvider
import models.beneficiary.BeneficiaryType
import models._
import pages.beneficiary.BeneficiaryTypePage
import play.api.data.Form
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.Future

class BeneficiaryTypeControllerSpec extends SpecBase {

  private val formProvider = new BeneficiaryTypeFormProvider()
  private val form: Form[BeneficiaryType] = formProvider()

  private lazy val beneficiaryTypeRoute: String =
    controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, testIndex, NormalMode).url

  "BeneficiaryTypeController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryTypeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BeneficiaryTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, testIndex, NormalMode, false)(using
          request,
          messages(application)
        ).toString
      }
    }

    testInvalidBeneficiaryIndexes.foreach { invalidIndex =>
      s"must return Not found for invalid index $invalidIndex  in the URL" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, invalidIndex, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryTypeRoute)

        val view = application.injector.instanceOf[BeneficiaryTypeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(BeneficiaryType.Individual),
          srn,
          testIndex,
          NormalMode,
          false
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the CYA page when submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, beneficiaryTypeRoute)
            .withFormUrlEncodedBody(("value", BeneficiaryType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, beneficiaryTypeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BeneficiaryTypeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, testIndex, NormalMode, false)(using
          request,
          messages(application)
        ).toString
      }
    }
  }
}
