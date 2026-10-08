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
import services.UserAnswersService
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import play.api.inject.bind
import base.SpecBase
import models.beneficiary.BeneficiaryType
import models.{CheckMode, JourneyRole, NormalMode}
import pages.beneficiary.{BeneficiaryNamePage, BeneficiaryTypePage}
import play.api.test.Helpers._
import org.mockito.Mockito.verifyNoInteractions

class BeneficiaryAccessSpec extends SpecBase {

  private val answersChangedToNo = emptyUserAnswers
    .set(DidPrSubmitPage, true).success.value
    .set(AreBeneficiariesKnownPage, true).success.value
    .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual).success.value
    .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), individualName).success.value
    .set(AreBeneficiariesKnownPage, false).success.value

  private lazy val beneficiaryPages = Seq(NormalMode, CheckMode).flatMap { mode =>
    Seq(
      routes.BeneficiaryTypeController.onPageLoad(srn, testIndex, mode).url ->
        Seq("value" -> BeneficiaryType.Individual.toString),
      routes.BeneficiaryNameController.onPageLoad(srn, mode, testIndex).url ->
        Seq("firstForename" -> "Jane", "surname" -> "Smith"),
      routes.BeneficiaryTrustNameController.onPageLoad(srn, testIndex, mode).url ->
        Seq("value" -> trustName),
      routes.BeneficiaryHasNinoController.onPageLoad(srn, testIndex, mode).url ->
        Seq("value" -> "false"),
      routes.RemoveBeneficiaryController.onPageLoad(srn, mode, testIndex).url ->
        Seq("value" -> "true")
    )
  } :+ (routes.BeneficiaryListController.onPageLoad(srn).url -> Seq("value" -> "true"))

  Seq(GET, POST).foreach { method =>
    s"must redirect beneficiary $method requests to CYA without saving after the answer changes to No" in {
      val userAnswersService = mock[UserAnswersService]
      val application = applicationBuilder(userAnswers = Some(answersChangedToNo), usesSession = true)
        .overrides(bind[UserAnswersService].toInstance(userAnswersService))
        .build()

      running(application) {
        beneficiaryPages.foreach { case (url, fields) =>
          withClue(s"$method $url: ") {
            val request = FakeRequest(method, url).withFormUrlEncodedBody(fields*)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.CheckYourAnswersController.onPageLoad(srn).url
          }
        }
        verifyNoInteractions(userAnswersService)
      }
    }
  }
}
