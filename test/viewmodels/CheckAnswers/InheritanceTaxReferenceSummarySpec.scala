package viewmodels.CheckAnswers

import pages.{IHTPaymentReferencePage, InheritanceTaxReferencePage}
import base.SpecBase
import models.CheckMode
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, Value}

class InheritanceTaxReferenceSummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "InheritanceTaxReferenceSummary" - {

    "must return None when data is not present" in {
      val result = InheritanceTaxReferenceSummary.row(srn, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when the inheritance tax reference is present" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, "1234567890")
        .success
        .value

      val result = InheritanceTaxReferenceSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("inheritanceTaxReference.checkYourAnswersLabel"))
      result.value.value mustEqual Value(HtmlContent("1234567890"))
      result.value.actions.value.items must contain(
        ActionItem(
          controllers.routes.InheritanceTaxReferenceController.onPageLoad(srn, CheckMode).url,
          Text("site.change"),
          visuallyHiddenText = Some("inheritanceTaxReference.change.hidden")
        )
      )
    }

    "must return a row when the inheritance tax reference is present with payment reference" in {
      val userAnswers = emptyUserAnswers
        .set(IHTPaymentReferencePage, "paymentRef")
        .get
        .set(InheritanceTaxReferencePage, "1234567890")
        .success
        .value

      val result = InheritanceTaxReferenceSummary.row(srn, userAnswers)

      result.value.key mustEqual Key(Text("inheritanceTaxReference.checkYourAnswersLabel"))
      result.value.value mustEqual Value(HtmlContent("1234567890"))
      result.value.actions.value.items must not contain ActionItem(
        controllers.routes.InheritanceTaxReferenceController.onPageLoad(srn, CheckMode).url,
        Text("site.change"),
        visuallyHiddenText = Some("inheritanceTaxReference.change.hidden")
      )
    }
  }
}
