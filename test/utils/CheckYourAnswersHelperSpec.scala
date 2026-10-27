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

package utils

import models.JourneyRole._
import pages._
import controllers.routes
import base.SpecBase
import models.beneficiary.BeneficiaryType
import models._
import play.api.test.Helpers.running
import org.scalatest.freespec.AnyFreeSpec

class CheckYourAnswersHelperSpec extends AnyFreeSpec with SpecBase {

  "buildSummaryLists must place beneficiary country before address with both links restarting ALF" in {
    runningApplication { app =>
      given play.api.i18n.Messages = messages(app)
      val lists = CheckYourAnswersHelper.buildSummaryLists(
        individualBeneficiaryUserAnswers,
        srn,
        app.injector.instanceOf[services.CountryService].nameForCode,
        app.injector.instanceOf[play.api.i18n.MessagesApi]
      )
      val rows = lists.beneficiaryList.head.rows
      val countryIndex = rows.indexWhere(
        _.key.content == uk.gov.hmrc.govukfrontend.views.Aliases
          .Text(messages(app)("beneficiaryCountry.checkYourAnswersLabel"))
      )
      countryIndex must be >= 0
      val addressRow = rows(countryIndex + 1)
      addressRow.key.content mustBe uk.gov.hmrc.govukfrontend.views.Aliases.Text(
        messages(app)("beneficiaryAddress.checkYourAnswersLabel")
      )
      val startUrl = controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, 0, CheckMode).url
      rows(countryIndex).actions.value.items.head.href mustBe startUrl
      addressRow.actions.value.items.head.href mustBe startUrl
    }
  }

  "findPageToContinue" - {

    Seq(BeneficiaryType.Individual, BeneficiaryType.Trust).foreach { beneficiaryType =>
      s"resume address lookup for a $beneficiaryType with no saved address" in {
        val role = if (beneficiaryType == BeneficiaryType.Individual) BeneficiaryIndividual else BeneficiaryTrust
        val answers = (for {
          typed <- prIndividualUserAnswers.set(pages.beneficiary.BeneficiaryTypePage(0), beneficiaryType)
          contact <- typed.set(pages.beneficiary.BeneficiaryNamePage(0, role), individualName)
          completed <-
            if (beneficiaryType == BeneficiaryType.Trust) {
              contact.set(pages.beneficiary.BeneficiaryTrustNamePage(0), trustName)
            } else {
              contact
                .set(pages.beneficiary.BeneficiaryHasNinoPage(0), true)
                .flatMap(_.set(pages.beneficiary.BeneficiaryNinoPage(0), testNino))
            }
        } yield completed).success.value

        CheckYourAnswersHelper.findPageToContinue(answers, srn).value mustBe
          controllers.beneficiary.routes.BeneficiaryAddressController.start(srn, 0, NormalMode)
      }
    }

    "must return the IndividualName(Deceased) page if a value is missing" in {
      val userAnswers = deceasedPrUserAnswers
        .remove(IndividualNamePage(Deceased))
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.IndividualNameController.onPageLoad(srn, NormalMode, Deceased).url
      )
    }

    "must return the hasNino page if a value is missing" in {
      val userAnswers = deceasedPrUserAnswers
        .remove(HasNinoPage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.HasNinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the nino page if a value is missing and hasNino is true" in {
      val userAnswers = deceasedPrUserAnswers
        .set(HasNinoPage, true)
        .get
        .remove(NinoPage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the noNinoReason page if a value is missing and hasNino is false" in {
      val userAnswers = deceasedPrUserAnswers
        .set(HasNinoPage, false)
        .get
        .remove(NoNinoReasonPage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NoNinoReasonController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the BirthDeathDates page if a value is missing" in {
      val userAnswers = deceasedPrUserAnswers
        .remove(BirthDeathDatesPage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.BirthDeathDatesController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the PrType page if a value is missing" in {
      val userAnswers = deceasedPrUserAnswers
        .remove(PrTypePage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.PrTypeController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the individualName page if a value is missing" in {
      val userAnswers = prIndividualUserAnswers
        .remove(IndividualNamePage(JourneyRole.PrIndividual))
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrIndividual).url
      )
    }

    "must return the addressLookUp page if a value is missing" in {
      val userAnswers = prIndividualUserAnswersNoAddress

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.AddressLookupStartController.start(srn, NormalMode, PrIndividual).url
      )
    }

    "must return the organisationName page if a value is missing" in {
      val userAnswers = prOrganisationUserAnswers
        .remove(OrganisationNamePage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.OrganisationNameController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the individualName(Organisation) page if a value is missing" in {
      val userAnswers = prOrganisationUserAnswers
        .remove(IndividualNamePage(PrOrganisation))
        .get
        .set(OrganisationNamePage, "Organisation Name")
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrOrganisation).url
      )
    }

    "must return the addressLookUp page page if a value is missing" in {
      val userAnswers = prOrganisationUserAnswersNoAddress

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.AddressLookupStartController.start(srn, NormalMode, PrOrganisation).url
      )
    }

    "must return the DidPrSubmit page if a value is missing" in {
      val userAnswers = prIndividualUserAnswers
        .remove(DidPrSubmitPage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the PaymentNoticeDate page if a value is missing" in {
      val userAnswers = prIndividualUserAnswers
        .remove(PaymentNoticeDatePage)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the first beneficiary type page if are beneficiaries are know is yes and type of first is missing" in {
      val userAnswers = prIndividualUserAnswers

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode).url
      )
    }

    "must return the BeneficiaryName page if a value is missing" in {
      val userAnswers = prIndividualUserAnswers
        .set(pages.beneficiary.BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        controllers.beneficiary.routes.BeneficiaryNameController
          .onPageLoad(srn, NormalMode, 0, BeneficiaryIndividual)
          .url
      )
    }

    "must return the None if no values are missing" in {
      val userAnswers = prIndividualUserAnswers
        .set(pages.beneficiary.BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .get
        .set(
          pages.beneficiary.BeneficiaryNamePage(0, BeneficiaryIndividual),
          individualName
        )
        .get
        .set(pages.beneficiary.BeneficiaryHasNinoPage(0), true)
        .get
        .set(pages.beneficiary.BeneficiaryNinoPage(0), testNino)
        .get
        .set(pages.beneficiary.BeneficiaryIhtPayablePage(0), BigDecimal("12.34"))
        .get
        .set(pages.beneficiary.BeneficiaryInterestPayablePage(0), BigDecimal("1.23"))
        .get
        .set(pages.beneficiary.BeneficiaryAddressPage(0), testPrAddress)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn) mustBe None
    }

    "must resume at the first individual beneficiary with an unanswered amount and include saved amounts in CYA" in {
      val completeFirst = prIndividualUserAnswers
        .set(pages.beneficiary.BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryNamePage(0, BeneficiaryIndividual), individualName)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryHasNinoPage(0), false)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryNoNinoReasonPage(0), testReasonNoNino)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryIhtPayablePage(0), BigDecimal("1234.56"))
        .success
        .value
        .set(pages.beneficiary.BeneficiaryInterestPayablePage(0), BigDecimal("56.78"))
        .success
        .value
        .set(pages.beneficiary.BeneficiaryAddressPage(0), testPrAddress)
        .success
        .value
      val missingNino = completeFirst
        .set(pages.beneficiary.BeneficiaryTypePage(1), BeneficiaryType.Individual)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryNamePage(1, BeneficiaryIndividual), individualName)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryHasNinoPage(1), true)
        .success
        .value
        .set(pages.beneficiary.BeneficiaryAddressPage(1), testPrAddress)
        .success
        .value
      CheckYourAnswersHelper.findPageToContinue(missingNino, srn).value mustBe
        controllers.beneficiary.routes.BeneficiaryNinoController.onPageLoad(srn, 1, NormalMode)
      val missingReason = missingNino.set(pages.beneficiary.BeneficiaryHasNinoPage(1), false).success.value
      CheckYourAnswersHelper.findPageToContinue(missingReason, srn).value mustBe
        controllers.beneficiary.routes.BeneficiaryNoNinoReasonController.onPageLoad(srn, 1, NormalMode)
      val incompleteSecond = missingNino.set(pages.beneficiary.BeneficiaryNinoPage(1), testNino).success.value
      CheckYourAnswersHelper.findPageToContinue(incompleteSecond, srn).value mustBe
        controllers.beneficiary.routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, NormalMode)
      CheckYourAnswersHelper.findBeneficiaryPageToContinue(incompleteSecond, srn).value mustBe
        controllers.beneficiary.routes.BeneficiaryIhtPayableController.onPageLoad(srn, 1, NormalMode)

      val app = applicationBuilder().build()
      running(app) {
        given play.api.i18n.Messages = messages(app)
        val lists = CheckYourAnswersHelper.buildSummaryLists(
          incompleteSecond,
          srn,
          identity,
          app.injector.instanceOf[play.api.i18n.MessagesApi]
        )
        lists.beneficiaryList.head.rows.takeRight(2).map(_.value.content) mustBe Seq(
          uk.gov.hmrc.govukfrontend.views.Aliases.Text("\u00a31,234.56"),
          uk.gov.hmrc.govukfrontend.views.Aliases.Text("\u00a356.78")
        )
      }
    }
  }
}
