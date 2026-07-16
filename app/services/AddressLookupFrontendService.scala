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

package services

import play.api.mvc.RequestHeader
import connectors.AddressLookupFrontendConnector
import config.FrontendAppConfig
import models.SchemeId.Srn
import models.addresslookup._
import uk.gov.hmrc.http.HeaderCarrier
import models.{JourneyRole, Mode}
import play.api.i18n.MessagesApi

import scala.concurrent.Future

import javax.inject.Inject

class AddressLookupFrontendService @Inject() (
  config: FrontendAppConfig,
  connector: AddressLookupFrontendConnector,
  messagesApi: MessagesApi,
  countryService: CountryService
) {

  def initJourney(srn: Srn, mode: Mode, name: String, journeyRole: JourneyRole)(implicit
    hc: HeaderCarrier,
    request: RequestHeader
  ): Future[String] =
    connector.initJourney(srn, mode, journeyConfig(srn, mode, name, journeyRole), journeyRole)

  def getAddress(addressId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData] =
    connector.getAddress(addressId)

  private def journeyConfig(srn: Srn, mode: Mode, name: String, journeyRole: JourneyRole)(implicit
    request: RequestHeader
  ): AlfJourneyConfig =
    AlfJourneyConfig(
      options = AlfOptions(
        continueUrl = config.addressLookupContinueUrl(srn, mode, journeyRole),
        signOutHref = config.signOutSurveyUrl,
        deskProServiceName = config.appName,
        phaseFeedbackLink = config.feedbackUrl,
        allowedCountryCodes = Some(countryService.countries.map(_.code)),
        manualAddressEntryConfig = AlfManualAddressEntryConfig(
          mandatoryFields = AlfMandatoryFields()
        )
      ),
      labels = AlfLabelsConfig(labels(name, journeyRole))
    )

  private def message(key: String, args: Any*): String =
    messagesApi.preferred(Seq.empty)(key, args*)

  private def labels(name: String, journeyRole: JourneyRole): AlfLabels =
    AlfLabels(
      appLevelLabels = AlfAppLabels(
        navTitle = message("service.name"),
        phaseBannerHtml = message("addressLookup.phaseBannerHtml")
      ),
      countryPickerLabels = AlfCountryPickerLabels(
        title = message("addressLookup.countryPicker.title", name),
        heading = message(s"addressLookup.countryPicker.${journeyRole.key}.heading", name),
        countryLabel = message("addressLookup.countryPicker.countryLabel"),
        submitLabel = message("site.saveAndContinue")
      ),
      selectPageLabels = AlfSelectPageLabels(
        title = message("addressLookup.select.title", name),
        heading = message("addressLookup.select.heading", name),
        submitLabel = message("site.saveAndContinue"),
        editAddressLinkText = message("addressLookup.select.editAddressLinkText")
      ),
      lookupPageLabels = AlfLookupPageLabels(
        title = message(s"addressLookup.lookup.${journeyRole.key}.title", name),
        heading = message(s"addressLookup.lookup.heading", name),
        filterLabel = message("addressLookup.lookup.filterLabel"),
        postcodeLabel = message("addressLookup.lookup.postcode"),
        submitLabel = message("site.saveAndContinue")
      ),
      confirmPageLabels = AlfConfirmPageLabels(
        title = message("addressLookup.confirm.title", name),
        heading = message("addressLookup.confirm.heading", name),
        submitLabel = message("addressLookup.confirm.submitLabel")
      ),
      editPageLabels = AlfEditPageLabels(
        title = message(s"addressLookup.edit.${journeyRole.key}.title", name),
        heading = message("addressLookup.edit.heading", name),
        line1Label = message("addressLookup.edit.line1Label"),
        line2Label = message("addressLookup.edit.line2Label"),
        line3Label = message("addressLookup.edit.line3Label"),
        townLabel = message("addressLookup.edit.townLabel"),
        postcodeLabel = message("addressLookup.edit.postcodeLabel"),
        countryLabel = message("addressLookup.edit.countryLabel"),
        submitLabel = message("site.saveAndContinue")
      ),
      international = AlfInternationalLabels(
        editPageLabels = AlfEditPageLabels(
          title = message(s"addressLookup.international.edit.${journeyRole.key}.title", name),
          heading = message("addressLookup.international.edit.heading", name),
          line1Label = message("addressLookup.international.edit.line1Label"),
          line2Label = message("addressLookup.international.edit.line2Label"),
          line3Label = message("addressLookup.international.edit.line3Label"),
          townLabel = message("addressLookup.international.edit.townLabel"),
          postcodeLabel = message("addressLookup.international.edit.postcodeLabel"),
          countryLabel = message("addressLookup.international.edit.countryLabel"),
          submitLabel = message("site.saveAndContinue")
        )
      )
    )
}
