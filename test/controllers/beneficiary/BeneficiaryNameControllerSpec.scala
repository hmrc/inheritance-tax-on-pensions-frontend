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

import base.beneficiary.BeneficiarySpecBase
import connectors.InheritanceTaxOnPensionsConnector
import controllers.routes
import play.api.inject.bind
import views.html.beneficiary.BeneficiaryNameView
import forms.beneficiary.BeneficiaryNameFormProvider
import controllers.beneficiary.BeneficiaryNameController
import models._
import pages.beneficiary.{BeneficiaryHasNinoPage, BeneficiaryNamePage}
import org.mockito.ArgumentMatchers._
import models.JourneyRole.{BeneficiaryIndividual, BeneficiaryTrust}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

class BeneficiaryNameControllerSpec extends BeneficiarySpecBase {

  private val formProvider = new BeneficiaryNameFormProvider()

  private val individualName = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Surname"
  )

  private case class JourneyRoleTestCase(
    journeyRole: JourneyRole,
    nextPageUrl: String
  )

  private lazy val journeyRoleTestCases = Seq(
    JourneyRoleTestCase(
      JourneyRole.BeneficiaryIndividual,
      controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, testIndex, NormalMode).url
    ),
    JourneyRoleTestCase(
      JourneyRole.BeneficiaryTrust,
      controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, testIndex, NormalMode).url
    )
  )

  "BeneficiaryNameController Controller" - {

    journeyRoleTestCases.foreach { testCase =>
      val journeyRole = testCase.journeyRole

      s"must return OK and the correct view for a GET for ${journeyRole.name}" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(GET, "/test-only/beneficiary-name")
          val controller = application.injector.instanceOf[BeneficiaryNameController]

          val result = controller.onPageLoad(srn, NormalMode, 0, journeyRole)(request)

          val view = application.injector.instanceOf[BeneficiaryNameView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formProvider(journeyRole), srn, 0, NormalMode, journeyRole)(using
            request,
            messages(application)
          ).toString
        }
      }

      s"must populate the view correctly on a GET when ${journeyRole.name} has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId, srnGen.sample.value.toString, testUuid)
          .set(BeneficiaryNamePage(0, journeyRole), individualName)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(GET, "/test-only/beneficiary-name")
          val controller = application.injector.instanceOf[BeneficiaryNameController]

          val view = application.injector.instanceOf[BeneficiaryNameView]

          val result = controller.onPageLoad(srn, NormalMode, 0, journeyRole)(request)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formProvider(journeyRole).fill(individualName),
            srn,
            0,
            NormalMode,
            journeyRole
          )(using request, messages(application)).toString
        }
      }

      s"must redirect to the correct next page when valid ${journeyRole.name} data is submitted" in {

        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(userAnswersWithBeneficiaries)))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaries), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val controller = application.injector.instanceOf[BeneficiaryNameController]
          val request =
            FakeRequest(POST, "/test-only/beneficiary-name")
              .withFormUrlEncodedBody(validFormData*)

          val result = controller.onSubmit(srn, NormalMode, 0, journeyRole)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must endWith(testCase.nextPageUrl)
        }
      }

      s"must return a Bad Request and errors when invalid ${journeyRole.name} data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

        running(application) {
          val controller = application.injector.instanceOf[BeneficiaryNameController]
          val request =
            FakeRequest(POST, "/test-only/beneficiary-name")
              .withFormUrlEncodedBody(invalidFormData*)

          val boundForm = formProvider(journeyRole).bind(invalidFormData.toMap)

          val view = application.injector.instanceOf[BeneficiaryNameView]

          val result = controller.onSubmit(srn, NormalMode, 0, journeyRole)(request)

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, srn, 0, NormalMode, journeyRole)(using
            request,
            messages(application)
          ).toString
        }
      }
    }

    s"must redirect to the beneficiary NINO page when valid beneficiary data is submitted in CheckMode and the NINO question is unanswered" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithBeneficiaries)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaries), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.beneficiary.routes.BeneficiaryNameController
              .onSubmit(srn, CheckMode, 0, BeneficiaryIndividual)
              .url
          )
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.beneficiary.routes.BeneficiaryHasNinoController
          .onPageLoad(srn, testIndex, CheckMode)
          .url
      }
    }

    "must redirect to the CYA page when valid beneficiary data is submitted in CheckMode and the NINO question is answered" in {

      val userAnswers = emptyUserAnswers.set(BeneficiaryHasNinoPage(testIndex), true).success.value
      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.beneficiary.routes.BeneficiaryNameController
              .onSubmit(srn, CheckMode, testIndex, BeneficiaryIndividual)
              .url
          )
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url
      }
    }

    "must redirect to address lookup when valid beneficiary trust data is submitted in NormalMode" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithBeneficiaries)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaries), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.beneficiary.routes.BeneficiaryNameController
              .onSubmit(srn, NormalMode, testIndex, BeneficiaryTrust)
              .url
          )
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.beneficiary.routes.BeneficiaryAddressController
          .start(srn, testIndex, NormalMode)
          .url
      }
    }

    "must collect a missing address when valid beneficiary trust data is submitted in CheckMode" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithBeneficiaries)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBeneficiaries), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.beneficiary.routes.BeneficiaryNameController
              .onSubmit(srn, CheckMode, testIndex, BeneficiaryTrust)
              .url
          )
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.beneficiary.routes.BeneficiaryAddressController
          .start(srn, testIndex, CheckMode)
          .url
      }
    }

    "must redirect to Journey Recovery for a GET when the journey role is unknown" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, "/test-only/unknown-name-page")

        val controller = application.injector.instanceOf[BeneficiaryNameController]

        val result = controller.onPageLoad(srn, NormalMode, 0, JourneyRole.Unknown)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when the journey role is unknown" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, "/test-only/unknown-name-page")
            .withFormUrlEncodedBody(validFormData*)

        val controller = application.injector.instanceOf[BeneficiaryNameController]

        val result = controller.onSubmit(srn, NormalMode, 0, JourneyRole.Unknown)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for an unsupported next page state" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val controller = application.injector.instanceOf[BeneficiaryNameController]

        controller.nextPage(
          srn,
          testIndex,
          NormalMode,
          JourneyRole.Unknown,
          emptyUserAnswers
        ) mustEqual routes.JourneyRecoveryController
          .onPageLoad()
      }
    }

    "must preserve the trust name when updating the trust representative name" in {

      val updatedName = IndividualName(
        title = Some("Dr"),
        firstForename = "Firstnametwo",
        secondForename = None,
        surname = "Surname"
      )

      val index = 1

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithMultipleBeneficiaries), usesSession = true).build()

      running(application) {
        val controller = application.injector.instanceOf[BeneficiaryNameController]

        val result =
          controller
            .addBeneficiaryName(userAnswersWithMultipleBeneficiaries, index, BeneficiaryTrust, updatedName)
            .success
            .value

        val trustPath = (result.data \ "beneficiaries")(index) \ "beneficiaryDetails" \ "trust"

        (trustPath \ "beneficiaryTrustName").as[String] mustEqual "trust"
        (trustPath \ "title").as[String] mustEqual "Dr"
        (trustPath \ "firstForename").as[String] mustEqual "Firstnametwo"
        (trustPath \ "secondForename").asOpt[String] mustBe None
        (trustPath \ "surname").as[String] mustEqual "Surname"
      }
    }

  }

  private val validFormData = Seq(
    "title" -> "Mr",
    "firstForename" -> "Firstname",
    "secondForename" -> "Middlename",
    "surname" -> "Surname"
  )

  private val invalidFormData = Seq(
    "title" -> "",
    "firstForename" -> "",
    "secondForename" -> "",
    "surname" -> ""
  )
}
