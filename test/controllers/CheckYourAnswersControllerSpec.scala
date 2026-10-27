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

import services.ReportRetrievalService
import pages._
import viewmodels.CheckAnswers.beneficiary.{BeneficiaryHasNinoSummary, BeneficiaryTypeSummary}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryList}
import viewmodels.govuk.all.{ActionItemViewModel, CardViewModel, SummaryListViewModel}
import play.api.libs.json.{JsPath, Json}
import models._
import viewmodels.CheckAnswers.{DidPrSubmitSummary, _}
import org.mockito.ArgumentMatchers.any
import models.JourneyRole.{BeneficiaryIndividual, Deceased, PrIndividual}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.when
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase
import models.beneficiary.BeneficiaryType
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

import java.time.LocalDate

class CheckYourAnswersControllerSpec extends SpecBase {

  private val validNino: String = ninoGen.sample.value
  private val emptySummaryList = SummaryListViewModel(rows = Seq())
  private val emptyBeneficiarySummaryListViewModel = List[SummaryList]()

  private def onPageLoadFakeRequest =
    FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(srn).url)
  private def onPageLoadContinueFakeRequest =
    FakeRequest(GET, routes.CheckYourAnswersController.onPageLoadContinueMode(srn).url)
  private def onPageLoadChangeFakeRequest =
    FakeRequest(GET, routes.CheckYourAnswersController.onPageLoadChangeMode(srn).url)

  "CheckYourAnswers Controller" - {
    "onPageLoad redirects to onPageLoadContinueMode when data is incomplete" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, inheritanceTaxReference)
        .success
        .value
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .get

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          onPageLoadFakeRequest

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoadContinueMode(srn).url
      }
    }

    "onPageLoad redirects to onPageLoadChangeMode when data has ihtPaymentReference and data is unchanged" in {
      val userAnswers = prIndividualUserAnswers
        .set(IHTPaymentReferencePage, "paymentRef")
        .get
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
        .set(pages.beneficiary.BeneficiaryAddressPage(0), testPrAddress)
        .get

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          onPageLoadFakeRequest

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoadChangeMode(srn).url
      }
    }

    "onPageLoadContinueMode" - {
      "must return OK and the correct view for a GET on change mode" in {
        val userAnswers = prIndividualUserAnswersNoAddress
          .set(IHTPaymentReferencePage, "paymentRef")
          .get

        userAnswers.data.setObject(JsPath \ "ihtVersion", Json.toJson("1.0"))

        val mockReportRetrievalService = mock[ReportRetrievalService]
        when(mockReportRetrievalService.getReport(any(), any())(using any(), any()))
          .thenReturn(
            Future.successful(
              Right(
                HttpResponse(
                  200,
                  Json
                    .obj(
                      "data" -> userAnswers.data
                    )
                    .toString()
                )
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[ReportRetrievalService].toInstance(mockReportRetrievalService))
          .build()

        running(application) {
          val request =
            onPageLoadChangeFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
              NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
              HasNinoSummary.row(srn, userAnswers)(using messages(application)).get,
              NoNinoReasonSummary.row(srn, userAnswers)(using messages(application)).get,
              BirthDeathDatesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get,
              PaymentNoticeDateSummary.row(srn, userAnswers)(using messages(application)).get,
              AreBeneficiariesKnownSummary.row(srn, userAnswers)(using messages(application)).get,
              NumberOfBeneficiariesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            None,
            submitBlocked = true,
            Some("testSchemeName")
          )(using
            request,
            messages(application)
          ).toString
        }
      }
    }

    "onPageLoadContinueMode" - {
      "must return OK and the correct view for a GET" in {
        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, inheritanceTaxReference)
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, true)
          .get
          .set(NinoPage, validNino)
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
              NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
              HasNinoSummary.row(srn, userAnswers)(using messages(application)).get,
              NinoSummary.row(srn, userAnswers)(using messages(application)).get,
              BirthDeathDatesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.AddressLookupStartController.start(srn = srn, mode = NormalMode, journeyRole = PrIndividual)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with No Nino reason" in {
        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, inheritanceTaxReference)
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, false)
          .get
          .set(NoNinoReasonPage, testReasonNoNino)
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
              NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
              HasNinoSummary.row(srn, userAnswers)(using messages(application)).get,
              NoNinoReasonSummary.row(srn, userAnswers)(using messages(application)).get,
              BirthDeathDatesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.AddressLookupStartController.start(srn = srn, mode = NormalMode, journeyRole = PrIndividual)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and include the payment notice date row when present" in {
        val userAnswers = emptyUserAnswers
          .set(DidPrSubmitPage, false)
          .success
          .value
          .set(PaymentNoticeDatePage, LocalDate.of(2026, 3, 27))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = emptySummaryList
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get,
              PaymentNoticeDateSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with individual PR details and address" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "prDetails" -> Json.obj(
                "individual" -> Json.obj(
                  "title" -> "Ms",
                  "firstForename" -> "Firstnametwo",
                  "secondForename" -> "Middlenametwo",
                  "surname" -> "Surname",
                  "addressLine1" -> "33 Fake Street",
                  "addressLine2" -> "AB Area",
                  "addressLine3" -> "Some District",
                  "addressLine4" -> "Anytown",
                  "postCode" -> "ZZ1 1ZZ",
                  "country" -> "GB"
                )
              )
            )
          )
          .set(PrTypePage, PrType.Individual)
          .success
          .value
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualCountrySummary
                .row(srn, userAnswers, (_: String) => "United Kingdom")(using messages(application))
                .get,
              PrIndividualAddressSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with organisation PR details and address" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "prDetails" -> Json.obj(
                "organisation" -> Json.obj(
                  "organisationName" -> "Test Organisation",
                  "title" -> "Ms",
                  "firstForename" -> "Firstnametwo",
                  "secondForename" -> "Middlenametwo",
                  "surname" -> "Surname",
                  "addressLine1" -> "33 Fake Street",
                  "addressLine2" -> "AB Area",
                  "addressLine3" -> "Some District",
                  "addressLine4" -> "Anytown",
                  "postCode" -> "ZZ1 1ZZ",
                  "country" -> "GB"
                )
              )
            )
          )
          .set(PrTypePage, PrType.Organisation)
          .success
          .value
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationPrNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationCountrySummary
                .row(srn, userAnswers, (_: String) => "United Kingdom")(using messages(application))
                .get,
              PrOrganisationAddressSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and view with beneficiaries for a GET" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "beneficiaries" -> Json.arr(
                Json.obj(
                  "beneficiaryType" -> "individual",
                  "hasNino" -> true
                ),
                Json.obj(
                  "beneficiaryType" -> "individual"
                )
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val beneficiarySummaryList = List(
            SummaryListViewModel(
              rows = Seq(
                BeneficiaryTypeSummary.row(srn, 0, userAnswers)(using messages(application)).get,
                BeneficiaryHasNinoSummary.row(srn, 0, userAnswers)(using messages(application)).get
              ),
              card = CardViewModel(
                messages(application)("checkYourAnswers.beneficiary.details.card.title", 1),
                2,
                Some(
                  Actions(
                    items = Seq(
                      ActionItemViewModel(
                        Text(messages(application)("site.remove")),
                        s"""/inheritance-tax-on-pensions/${srn.value}/change-remove-beneficiary/0"""
                      ).copy(visuallyHiddenText =
                        Some(messages(application)("checkYourAnswers.beneficiary.details.card.remove.hidden", 0))
                      )
                    )
                  )
                )
              )
            ),
            SummaryListViewModel(
              rows = Seq(
                BeneficiaryTypeSummary.row(srn, 1, userAnswers)(using messages(application)).get
              ),
              card = CardViewModel(
                messages(application)("checkYourAnswers.beneficiary.details.card.title", 2),
                2,
                Some(
                  Actions(
                    items = Seq(
                      ActionItemViewModel(
                        Text(messages(application)("site.remove")),
                        s"""/inheritance-tax-on-pensions/${srn.value}/change-remove-beneficiary/1"""
                      ).copy(visuallyHiddenText =
                        Some(messages(application)("checkYourAnswers.beneficiary.details.card.remove.hidden", 1))
                      )
                    )
                  )
                )
              )
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            emptySummaryList,
            emptySummaryList,
            emptySummaryList,
            beneficiarySummaryList,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased)),
            submitBlocked = false,
            None
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return OK when UUID is not in query parameter" in {

        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, inheritanceTaxReference)
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, true)
          .get
          .set(NinoPage, validNino)
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }
    "onSubmit" - {
      "must redirect to the psa confirmation page on post for psa" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PsaDeclarationController.onPageLoad(srn).url
        }
      }

      "must redirect to the psp confirmation page on post for psp" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), isPsa = false).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PspDeclarationController.onPageLoad(srn).url
        }
      }
    }
  }
}
