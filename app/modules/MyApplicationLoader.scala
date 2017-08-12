package modules

import _root_.controllers.AssetsComponents
import chat.ChatEngine
import com.softwaremill.macwire._
import play.api._
import play.engineio.EngineIOController
import play.socketio.scaladsl.SocketIOComponents
import router.Routes

class MyApplicationLoader extends ApplicationLoader {
  override def load(context: ApplicationLoader.Context): Application =
    new BuiltInComponentsFromContext(context) with MyApplication with NoHttpFiltersComponents {
      LoggerConfigurator.apply(context.environment.classLoader)
        .foreach(_.configure(context.environment))
    }.application
}

trait MyApplication extends BuiltInComponents
  with AssetsComponents
  with SocketIOComponents {

  lazy val chatEngine: ChatEngine = wire[ChatEngine]
  lazy val engineIOController: EngineIOController = chatEngine.controller

  override lazy val router: Routes = {
    val prefix = "/"
    wire[_root_.router.Routes]
  }
}