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

package controllers

import play.api.test.FakeRequest
import org.jsoup.Jsoup
import play.api.inject.bind
import base.SpecBase
import repositories.SessionSchemeDetailsRepository
import models.SessionSchemeDetails
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

import java.time.{Clock, Instant, ZoneOffset}

class JourneyRecoveryControllerSpec extends SpecBase {

  "JourneyRecovery Controller" - {

    "must show the error timestamp and continue to the current scheme's IHTP overview" in {
      val repository = mock[SessionSchemeDetailsRepository]
      when(repository.get(any())).thenReturn(
        Future.successful(Some(SessionSchemeDetails(userAnswersId, srn.value, defaultSchemeDetails)))
      )
      val clock = Clock.fixed(Instant.parse("2026-09-17T14:55:09Z"), ZoneOffset.UTC)
      val application = applicationBuilder()
        .overrides(bind[SessionSchemeDetailsRepository].toInstance(repository), bind[Clock].toInstance(clock))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)
        val result = route(application, request).value
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustEqual OK
        document.text() must include(
          messages(application)("journeyRecovery.startAgain.occurred", "3:55:09pm", "17 September 2026")
        )
        document.select(".govuk-button").attr("href") mustEqual routes.SubmissionListController.onPageLoad(srn).url
      }
    }

  }
}
