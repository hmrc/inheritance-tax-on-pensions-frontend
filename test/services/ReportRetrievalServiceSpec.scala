package services

import models.{IhtpOverviewResponse, IhtpOverviewSuccess}
import models.requests.AllowedAccessRequest
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Mockito._
import base.SpecBase
import connectors.InheritanceTaxOnPensionsConnector
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

class ReportRetrievalServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestNoEstablishersGen(FakeRequest()).sample.value

  "getReport" - {

    "must return success if connector returns success" in new Setup {
      val response = IhtpOverviewResponse(IhtpOverviewSuccess(Seq.empty))

      when(mockConnector.getReport(any(), any(), any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(response)))

      whenReady(testService.getReport("testPaymentReference", "testVersion")) {
        _ mustBe Right(response)
      }

      verify(mockConnector).getReport(
        eqTo(allowedAccessRequest.schemeDetails.pstr),
        eqTo("testPaymentReference"),
        eqTo("testVersion"),
        any(),
        any(),
        any(),
        any()
      )(using any())
    }

    class Setup {
      val mockConnector: InheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

      val testService: ReportRetrievalService = new ReportRetrievalService(mockConnector, mockAppConfig)
    }
  }
}
