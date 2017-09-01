package controllers

import play.api.mvc._
import javax.inject.Inject
import play.api.i18n.{ MessagesApi }

class Application @Inject() (val messagesApi: MessagesApi) extends api.ApiController {

  def profile = Action { request =>
    Ok("ok")
  }

  def test = ApiAction { implicit request =>
    ok("The API is ready")
  }

  def swagger = Action {
    request =>
      Ok(views.html.swagger())
  }

}
