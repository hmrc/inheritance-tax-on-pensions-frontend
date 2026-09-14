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

import config.FrontendAppConfig
import uk.gov.hmrc.http.UpstreamErrorResponse
import models._
import play.api.test.Helpers._
import org.mockito.Mockito.when
import uk.gov.hmrc.govukfrontend.views.Aliases.{PaginationItem, PaginationLink}
import base.SpecBase
import play.api.i18n.Messages
import viewmodels.SubmissionListPagination

import java.time.{Instant, LocalDate}

class SubmissionListUtilSpec extends SpecBase with org.scalatest.EitherValues {
  private val overviewReport = IhtpOverviewReport(
    uuid = None,
    fbNumber = Some("119000004320"),
    submissionDate = Some(Instant.parse("2026-04-10T16:12:49Z")),
    paymentDueDate = Some(LocalDate.of(2026, 2, 2)),
    ihtVersion = "001",
    inheritanceTaxReference = "A123456/25A",
    paymentReference = Some("A123456/25A629671"),
    title = Some("Dr"),
    firstForename = Some("Firstname"),
    secondForename = Some("M"),
    surname = Some("Surname"),
    nino = None,
    ihtpStatus = "Not reconciled"
  )

  private val config = mock[FrontendAppConfig]
  when(config.submissionListPageSize).thenReturn(2)
  private val util = new SubmissionListUtil(config)
  private val active = overviewReport
  private val paid = overviewReport.copy(ihtpStatus = "Paid")
  private def response(reports: Seq[IhtpOverviewReport]) = Right(IhtpOverviewResponse(IhtpOverviewSuccess(reports)))

  "buildPagination" - {
    implicit val messages: Messages = stubMessages()

    Seq(false, true).foreach { paidOnly =>
      (1 to 3).foreach { currentPage =>
        s"must build page links and navigation for paid=$paidOnly on page $currentPage" in {
          val listUrl =
            if (paidOnly) controllers.routes.PaidReportsController.onPageLoad(srn).url
            else controllers.routes.SubmissionListController.onPageLoad(srn).url
          val result = util.buildPagination(srn, paidOnly, SubmissionListPagination(currentPage, 2, 5))

          result.items.value mustBe (1 to 3).map { page =>
            PaginationItem(
              href = s"$listUrl?page=$page",
              number = Some(page.toString),
              visuallyHiddenText = Some(messages("submissionList.pagination.pageNumber", page)),
              current = if (page == currentPage) Some(true) else None
            )
          }
          result.previous mustBe Option.when(currentPage > 1)(
            PaginationLink(
              href = s"$listUrl?page=${currentPage - 1}",
              text = Some(messages("submissionList.pagination.previous"))
            )
          )
          result.next mustBe Option.when(currentPage < 3)(
            PaginationLink(
              href = s"$listUrl?page=${currentPage + 1}",
              text = Some(messages("submissionList.pagination.next"))
            )
          )
          result.landmarkLabel mustBe Some(messages("submissionList.pagination.label"))
        }
      }
    }

    Seq(0, 1, 2).foreach { totalReports =>
      s"must omit previous and next links for a single page with $totalReports reports" in {
        val result = util.buildPagination(srn, paid = true, SubmissionListPagination(1, 2, totalReports))
        result.previous mustBe None
        result.next mustBe None
        result.items.value.size mustBe 1
        result.items.value.head.current mustBe Some(true)
      }
    }
  }

  "prepare" - {
    "must partition paid and active reports including drafts" in {
      val draft = active.copy(ihtpStatus = "In progress")
      val data = response(Seq(paid, active, draft))
      util.prepare(data, paid = true, None).value._1 mustBe Seq(paid)
      util.prepare(data, paid = false, None).value._1 mustBe Seq(active, draft)
    }

    "must filter before calculating pages and totals" in {
      val data = response(Seq(active, paid, active, paid, active, paid))
      util.prepare(data, paid = true, Some("2")).value mustBe
        (Seq(paid), SubmissionListPagination(2, 2, 3))
      util.prepare(data, paid = false, Some("2")).value mustBe
        (Seq(active), SubmissionListPagination(2, 2, 3))
    }

    Seq(None, Some("invalid"), Some("0"), Some("-1"), Some("99999999999999")).foreach { page =>
      s"must default or clamp page $page to the first page" in {
        util.prepare(response(Seq(paid, paid, paid)), paid = true, page).value._2.currentPage mustBe 1
      }
    }

    "must clamp pages beyond the final page" in {
      util.prepare(response(Seq(paid, paid, paid)), paid = true, Some("99")).value._2.currentPage mustBe 2
    }

    "must clamp to a full final page without adding an extra page" in {
      val (reports, pagination) = util.prepare(response(Seq.fill(4)(paid)), paid = true, Some("99")).value
      reports mustBe Seq(paid, paid)
      pagination.currentPage mustBe 2
      pagination.totalPages mustBe 2
      pagination.firstItem mustBe 3
      pagination.lastItem mustBe 4
      pagination.hasNext mustBe false
    }

    "must return an empty first page when nothing matches" in {
      util.prepare(response(Seq(active)), paid = true, Some("99")).value mustBe
        (Seq.empty, SubmissionListPagination(1, 2, 0))
    }

    "must retain existing no-records handling and propagate other failures" in {
      util.prepare(Left(UpstreamErrorResponse("none", UNPROCESSABLE_ENTITY)), paid = true, None).value._1 mustBe empty
      val failure = UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR)
      util.prepare(Left(failure), paid = true, None) mustBe Left(failure)
    }
  }
}
