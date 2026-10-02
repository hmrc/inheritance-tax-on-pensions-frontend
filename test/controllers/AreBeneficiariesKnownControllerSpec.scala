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

import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import connectors.InheritanceTaxOnPensionsConnector
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import play.api.inject.bind
import views.html.AreBeneficiariesKnownView
import base.SpecBase
import models._
import pages.beneficiary._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import models.JourneyRole.BeneficiaryIndividual
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.when
import forms.AreBeneficiariesKnownFormProvider
import models.beneficiary.BeneficiaryType

import scala.concurrent.Future

class AreBeneficiariesKnownControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new AreBeneficiariesKnownFormProvider()
  private val form = formProvider()
  private val userAnswersWithDidPrSubmit = emptyUserAnswers.set(DidPrSubmitPage, true).success.value
  private val userAnswersWithDidPrNotSubmit = emptyUserAnswers.set(DidPrSubmitPage, false).success.value

  private lazy val areBeneficiariesKnownRoute: String =
    routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode).url

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, areBeneficiariesKnownRoute)

  def postRequest(
    value: String,
    url: String = areBeneficiariesKnownRoute
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, url).withFormUrlEncodedBody("value" -> value)

  "AreBeneficiariesKnown Controller" - {

    val unknownBeneficiaries = userAnswersWithDidPrSubmit
      .set(AreBeneficiariesKnownPage, false)
      .success
      .value
      .set(pages.IhtPayablePage, BigDecimal("100.00"))
      .success
      .value
    val individualWithoutName = userAnswersWithDidPrSubmit
      .set(AreBeneficiariesKnownPage, true)
      .success
      .value
      .set(BeneficiaryTypePage(0), BeneficiaryType.Individual)
      .success
      .value
    val completeIndividual = individualWithoutName
      .set(BeneficiaryNamePage(0, BeneficiaryIndividual), individualName)
      .success
      .value
      .set(BeneficiaryHasNinoPage(0), false)
      .success
      .value

    Seq(
      (
        "No changes to Yes without beneficiaries",
        unknownBeneficiaries,
        true,
        () => controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode)
      ),
      (
        "an individual name is missing",
        individualWithoutName,
        true,
        () => controllers.beneficiary.routes.BeneficiaryNameController.onPageLoad(srn, NormalMode, 0)
      ),
      (
        "Yes is unchanged with a complete individual",
        completeIndividual,
        true,
        () => routes.CheckYourAnswersController.onPageLoad(srn)
      ),
      ("Yes changes to No", completeIndividual, false, () => routes.IhtPayableController.onPageLoad(srn, CheckMode))
    ).foreach { case (description, answers, known, expected) =>
      val prSubmittedAnswers =
        if (known && answers.get(AreBeneficiariesKnownPage).contains(false)) Seq(true, false) else Seq(true)
      prSubmittedAnswers.foreach { didPrSubmit =>
        s"must route correctly in CheckMode when $description and PR submitted is $didPrSubmit" in {
          val userAnswers = answers.set(DidPrSubmitPage, didPrSubmit).success.value
          val service = mock[services.UserAnswersService]
          when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(userAnswers)))
          val application = applicationBuilder(Some(userAnswers), usesSession = true)
            .overrides(bind[services.UserAnswersService].toInstance(service))
            .build()
          running(application) {
            val result = route(
              application,
              postRequest(known.toString, routes.AreBeneficiariesKnownController.onSubmit(srn, CheckMode).url)
            ).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe expected().url
          }
        }
      }
    }

    "must return OK and the correct view for a GET when PR submitted the payment notice" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[AreBeneficiariesKnownView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when PR did not submit the payment notice" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDidPrNotSubmit), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[AreBeneficiariesKnownView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithDidPrSubmit.set(AreBeneficiariesKnownPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val view = application.injector.instanceOf[AreBeneficiariesKnownView]

        val result = route(application, getRequest).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), srn, NormalMode)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to beneficiary individual or org when Yes is submitted" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithDidPrSubmit)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result = route(application, postRequest("true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.beneficiary.routes.BeneficiaryTypeController
          .onPageLoad(srn, 0, NormalMode)
          .url
      }
    }

    "must redirect to IHT payable when No is submitted" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithDidPrSubmit)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result = route(application, postRequest("false")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IhtPayableController.onPageLoad(srn, NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true).build()

      running(application) {
        val request = postRequest("")
        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[AreBeneficiariesKnownView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val result = route(application, postRequest("true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if PR submit payment notice has not been answered" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if PR submit payment notice has not been answered" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, postRequest("true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
