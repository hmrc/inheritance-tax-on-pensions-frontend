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
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.Aliases.{Pagination, PaginationItem, PaginationLink}
import models.{IhtpOverviewReport, IhtpOverviewResponse}
import play.api.http.Status.UNPROCESSABLE_ENTITY
import uk.gov.hmrc.http.UpstreamErrorResponse
import play.api.i18n.Messages
import viewmodels.SubmissionListPagination

import javax.inject.Inject

class SubmissionListUtil @Inject() (appConfig: FrontendAppConfig) {

  def buildPagination(srn: Srn, paid: Boolean, pagination: SubmissionListPagination)(implicit
    messages: Messages
  ): Pagination = {
    val listUrl =
      if (paid) controllers.routes.PaidReportsController.onPageLoad(srn).url
      else controllers.routes.SubmissionListController.onPageLoad(srn).url

    def pageUrl(page: Int): String = s"$listUrl?page=$page"

    Pagination(
      items = Some((1 to pagination.totalPages).map { page =>
        PaginationItem(
          href = pageUrl(page),
          number = Some(page.toString),
          visuallyHiddenText = Some(messages("submissionList.pagination.pageNumber", page)),
          current = if (page == pagination.currentPage) Some(true) else None
        )
      }),
      previous = Option.when(pagination.hasPrevious)(
        PaginationLink(
          href = pageUrl(pagination.currentPage - 1),
          text = Some(messages("submissionList.pagination.previous"))
        )
      ),
      next = Option.when(pagination.hasNext)(
        PaginationLink(
          href = pageUrl(pagination.currentPage + 1),
          text = Some(messages("submissionList.pagination.next"))
        )
      ),
      landmarkLabel = Some(messages("submissionList.pagination.label"))
    )
  }

  def prepare(
    response: Either[UpstreamErrorResponse, IhtpOverviewResponse],
    paid: Boolean,
    page: Option[String]
  ): Either[UpstreamErrorResponse, (Seq[IhtpOverviewReport], SubmissionListPagination)] = {
    val reports = response match {
      case Right(value) => Right(value.success.ihtpOverview)
      case Left(error) if error.statusCode == UNPROCESSABLE_ENTITY => Right(Seq.empty[IhtpOverviewReport])
      case Left(error) => Left(error)
    }
    reports.map { allReports =>
      val filtered = allReports.filter { report =>
        val isPaid = report.ihtpStatus == "Paid"
        if (paid) isPaid else !isPaid
      }
      val pageSize = appConfig.submissionListPageSize
      val firstPage = SubmissionListPagination(1, pageSize, filtered.size)
      val currentPage = page.flatMap(_.toIntOption).getOrElse(1).max(1).min(firstPage.totalPages)
      val firstItemIndex = (currentPage - 1) * pageSize
      filtered.slice(firstItemIndex, firstItemIndex + pageSize) ->
        firstPage.copy(currentPage = currentPage)
    }
  }
}
