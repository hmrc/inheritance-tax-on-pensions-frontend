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

package config

import play.api.mvc.RequestHeader
import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.SchemeId.Srn
import play.api.Configuration
import models.{Mode, PensionSchemeId}

import scala.concurrent.duration.Duration

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  private val host: String = configuration.get[String]("host")
  private val servicePath: String = configuration.get[String]("service.path")
  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "inheritance-tax-on-pensions-frontend"

  val appName: String = configuration.get[String]("appName")
  val ifsTimeout: Duration = configuration.get[Duration]("ifs.timeout")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  object urls {
    val loginUrl: String = configuration.get[String]("urls.login")
    val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
    val signOutUrl: String = configuration.get[String]("urls.signOut")
    val pensionSchemeEnquiry: String = configuration.get[String]("urls.pensionSchemeEnquiry")

    object managePensionsSchemes {
      val baseUrl: String = configuration.get[String]("urls.manage-pension-schemes.baseUrl")
      val registerUrl: String = baseUrl + configuration.get[String]("urls.manage-pension-schemes.register")
      val adminOrPractitionerUrl: String =
        baseUrl + configuration.get[String]("urls.manage-pension-schemes.adminOrPractitioner")
      val contactHmrc: String = baseUrl + configuration.get[String]("urls.manage-pension-schemes.contactHmrc")
      val cannotAccessDeregistered: String =
        baseUrl + configuration.get[String]("urls.manage-pension-schemes.cannotAccessDeregistered")
      val schemeSummaryDashboard: String =
        configuration.get[String]("urls.manage-pension-schemes.schemeSummaryDashboard")
      val schemeSummaryPspDashboard: String =
        configuration.get[String]("urls.manage-pension-schemes.schemeSummaryPSPDashboard")
    }

    object pensionAdministrator {
      val baseUrl: String = configuration.get[String]("urls.pension-administrator.baseUrl")
      val updateContactDetails: String =
        baseUrl + configuration.get[String]("urls.pension-administrator.updateContactDetails")
    }

    object pensionPractitioner {
      val baseUrl: String = configuration.get[String]("urls.pension-practitioner.baseUrl")
      val updateContactDetails: String =
        baseUrl + configuration.get[String]("urls.pension-administrator.updateContactDetails")
    }
  }

  private val exitSurveyBaseUrl: String = configuration.get[String]("feedback-frontend.host")
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/inheritance-tax-on-pensions-frontend"

  val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val sessionTtl: Long = configuration.get[Int]("mongodb.sessionTtl")
  val submissionListPageSize: Int = configuration.get[Int]("submission-list.page-size")
  val submissionListDateFrom: String = configuration.get[String]("submission-list.date-from")
  val submissionListDateTo: String = configuration.get[String]("submission-list.date-to")

  val pensionsAdministrator: Service = configuration.get[Service]("microservice.services.pensionAdministrator")
  val pensionsScheme: Service = configuration.get[Service]("microservice.services.pensionsScheme")

  def getUserAnswersUrl(id: String): String =
    s"$inheritanceTaxOnPensionsHost/inheritance-tax-on-pensions/user-answers/$id"

  def setUserAnswersUrl(): String =
    s"$inheritanceTaxOnPensionsHost/inheritance-tax-on-pensions/user-answers"

  def getSubmitReportUrl(pstr: String, userAnswersId: String): String =
    s"$inheritanceTaxOnPensionsHost/inheritance-tax-on-pensions/$pstr/submit-report/$userAnswersId"

  def getSubmissionListUrl(pstr: String, dateFrom: String, dateTo: String): String =
    s"$inheritanceTaxOnPensionsHost/inheritance-tax-on-pensions/$pstr/submission-list?dateFrom=$dateFrom&dateTo=$dateTo"

  def schemeDashboardUrl(srn: Srn, pensionSchemeId: PensionSchemeId): String =
    if (pensionSchemeId.isPSP) {
      urls.managePensionsSchemes.baseUrl + urls.managePensionsSchemes.schemeSummaryPspDashboard.format(srn.value)
    } else {
      urls.managePensionsSchemes.baseUrl + urls.managePensionsSchemes.schemeSummaryDashboard.format(srn.value)
    }

  val addressLookupFrontendBaseUrl: String =
    configuration.get[Service]("microservice.services.addressLookupFrontend").baseUrl

  def addressLookupContinueUrl(srn: Srn, mode: Mode): String =
    s"$host${routes.AddressLookupContinueController.continue(srn, mode).url}"

  val signOutSurveyUrl: String =
    s"$host$servicePath${controllers.auth.routes.AuthController.signOut().url}"

  private val inheritanceTaxOnPensionsHost: String =
    configuration.get[Service]("microservice.services.inheritanceTaxOnPensions").baseUrl

}
