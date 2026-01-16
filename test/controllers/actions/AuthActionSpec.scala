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

package controllers.actions

import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import play.api.test.Helpers._
import play.api.mvc._
import com.google.inject.Inject
import connectors.SessionDataCacheConnector
import config.FrontendAppConfig
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core.authorise.Predicate
import base.SpecBase
import uk.gov.hmrc.auth.core._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionSpec extends SpecBase with StubPlayBodyParsersFactory {
  val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new MissingBearerToken),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.urls.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new BearerTokenExpired),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.urls.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the you-need-to-register page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe appConfig.urls.managePensionsSchemes.registerUrl
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the you-need-to-register page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe appConfig.urls.managePensionsSchemes.registerUrl
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the you-need-to-register page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe appConfig.urls.managePensionsSchemes.registerUrl
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the you-need-to-register page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(appConfig.urls.managePensionsSchemes.registerUrl)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the you-need-to-register page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierActionImpl(
            appConfig,
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            mockSessionDataCacheConnector,
            stubPlayBodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(appConfig.urls.managePensionsSchemes.registerUrl)
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}
