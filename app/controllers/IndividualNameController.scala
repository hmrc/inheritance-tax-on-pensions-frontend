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

import services.UserAnswersService
import play.api.mvc._
import pages.{IndividualNamePage, OrganisationNamePage}
import controllers.actions._
import play.api.libs.json._
import forms.IndividualNameFormProvider
import models._
import views.html.IndividualNameView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

import javax.inject.Inject

class IndividualNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: IndividualNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: IndividualNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            Redirect(routes.JourneyRecoveryController.onPageLoad())

          case _ =>
            val form = formProvider(journeyRole)
            val preparedForm = request.userAnswers.get(IndividualNamePage(journeyRole)) match {
              case None => form
              case Some(individualName) => form.fill(individualName)
            }

            Ok(view(preparedForm, srn, mode, journeyRole, organisationName(request.userAnswers, journeyRole)))
        }
      }

  def onSubmit(srn: Srn, mode: Mode, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

          case _ =>
            formProvider(journeyRole)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(formWithErrors, srn, mode, journeyRole, organisationName(request.userAnswers, journeyRole))
                    )
                  ),
                individualName =>
                  for {
                    updatedAnswers <- Future
                      .fromTry(addIndividualName(request.userAnswers, journeyRole, individualName))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, mode, journeyRole))
              )
        }
      }

  private[controllers] def addIndividualName(
    userAnswers: UserAnswers,
    journeyRole: JourneyRole,
    individualName: IndividualName
  ): Try[UserAnswers] =
    journeyRole match {
      case JourneyRole.LprIndividual =>
        addLprName(userAnswers, "individual", individualName)
      case JourneyRole.LprOrganisation =>
        addLprName(userAnswers, "organisation", individualName)
      case _ =>
        userAnswers.set(IndividualNamePage(journeyRole), individualName)
    }

  private def addLprName(
    userAnswers: UserAnswers,
    lprTypeKey: String,
    individualName: IndividualName
  ): Try[UserAnswers] =
    Success(
      userAnswers.copy(
        data = userAnswers.data.deepMerge(
          Json.obj(
            "lprDetails" -> Json.obj(
              lprTypeKey -> Json.obj(
                "title" -> optionalString(individualName.title),
                "firstForename" -> individualName.firstForename,
                "secondForename" -> optionalString(individualName.secondForename),
                "surname" -> individualName.surname
              )
            )
          )
        )
      )
    )

  private def organisationName(userAnswers: UserAnswers, journeyRole: JourneyRole): Option[String] =
    Option.when(journeyRole == JourneyRole.LprOrganisation)(userAnswers.get(OrganisationNamePage)).flatten

  private def optionalString(value: Option[String]): JsValue =
    value.filter(_.nonEmpty).map(JsString.apply).getOrElse(JsNull)

  private[controllers] def nextPage(srn: Srn, mode: Mode, journeyRole: JourneyRole): Call =
    mode match {
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
      case NormalMode if journeyRole == JourneyRole.Deceased =>
        routes.NinoOrReasonController.onPageLoad(srn, NormalMode)
      case NormalMode if journeyRole == JourneyRole.LprIndividual =>
        routes.AddressLookupStartController.start(srn, NormalMode)
      case NormalMode if journeyRole == JourneyRole.LprOrganisation =>
        routes.DidPrSubmitController.onPageLoad(srn, NormalMode)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
