/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.auth

import play.api.test.FakeRequest
import base.SpecBase
import repositories.{SessionMinimalDetailsRepository, SessionSchemeDetailsRepository}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import play.api.inject.bind
import config.FrontendAppConfig

import scala.concurrent.Future

import java.net.URLEncoder

class AuthControllerSpec extends SpecBase with MockitoSugar {

  "signOut" - {

    "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL" in {

      val mockSessionSchemeDetailsRepository = mock[SessionSchemeDetailsRepository]
      val mockSessionMinimalDetailsRepository: SessionMinimalDetailsRepository = mock[SessionMinimalDetailsRepository]

      when(mockSessionSchemeDetailsRepository.clear(any())).thenReturn(Future.successful(true))
      when(mockSessionMinimalDetailsRepository.clear(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(None)
          .overrides(
            bind[SessionSchemeDetailsRepository].toInstance(mockSessionSchemeDetailsRepository),
            bind[SessionMinimalDetailsRepository].toInstance(mockSessionMinimalDetailsRepository)
          )
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        val encodedContinueUrl = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.urls.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
        verify(mockSessionSchemeDetailsRepository, times(1)).clear(eqTo(userAnswersId))
        verify(mockSessionMinimalDetailsRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }

  "signOutNoSurvey" - {

    "must clear users answers and redirect to sign out, specifying SignedOut as the continue URL" in {

      val mockSessionSchemeDetailsRepository = mock[SessionSchemeDetailsRepository]
      val mockSessionMinimalDetailsRepository: SessionMinimalDetailsRepository = mock[SessionMinimalDetailsRepository]

      when(mockSessionSchemeDetailsRepository.clear(any())).thenReturn(Future.successful(true))
      when(mockSessionMinimalDetailsRepository.clear(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(None, isPsa = false)
          .overrides(
            bind[SessionSchemeDetailsRepository].toInstance(mockSessionSchemeDetailsRepository),
            bind[SessionMinimalDetailsRepository].toInstance(mockSessionMinimalDetailsRepository)
          )
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOutNoSurvey().url)

        val result = route(application, request).value

        val encodedContinueUrl = URLEncoder.encode(routes.SignedOutController.onPageLoad().url, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.urls.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
        verify(mockSessionSchemeDetailsRepository, times(1)).clear(eqTo(userAnswersId))
        verify(mockSessionMinimalDetailsRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }
}
