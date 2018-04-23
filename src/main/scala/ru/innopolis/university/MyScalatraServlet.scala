package ru.innopolis.university

import java.util.regex.Pattern

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import doobie.util.yolo

import info.mukel.telegrambot4s.api.declarative.{Action, ChannelPosts, Commands}
import info.mukel.telegrambot4s.api.{Polling, Extractors => $, _}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{ChatId, Message, User}
import org.scalatra._

import scala.collection._
import scala.io.Source

class MyScalatraServlet extends ScalatraServlet {

  //change Polling to Webhook and uncomment @port @webhookUrl after deployment
  object RandomBot extends TelegramBot with Polling with Commands with ChannelPosts {
    lazy val token: String = scala.util.Properties
      .envOrNone("BOT_TOKEN")
      .getOrElse(Source.fromFile("bot.token").getLines().mkString)

    val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", "jdbc:postgresql:postgres", "postgres", ""
    )
    val y: yolo.Yolo[IO] = xa.yolo

    import y._

    private val admins: mutable.Set[Int] = mutable.Set(212071762, 115879171)
    private var target_channel_id = ChatId(0)

    def isAdmin(user: User): Boolean = admins.synchronized {
      admins.contains(user.id)
    }

    def adminOnly(ok: Action[User])(implicit msg: Message): Unit = {
      msg.from.foreach {
        user =>
          if (isAdmin(user))
            ok(user)
      }
    }

    def adminOrElse(ok: Action[User])(noAccess: Action[User])(implicit msg: Message): Unit = {
      msg.from.foreach {
        user =>
          if (isAdmin(user))
            ok(user)
          else
            noAccess(user)
      }
    }

    onCommand("start") { implicit msg => reply("send nudes") } //TODO Good start msg
    onCommand("help") { implicit msg => reply("send nudes") } //TODO Good help message
    onCommand("whoiam") { implicit msg => reply(msg.source.toString) }

    onCommand("setup") {
      implicit msg =>
        withArgs {
          args =>
            if (args.isEmpty) {
              reply("Invalid argumentヽ(ಠ_ಠ)ノ, use /setup Credential e.g. Петров И")
            } else {
              val username = args.mkString(" ").toLowerCase
              val user_id = msg.source
              sql"insert into users (id, name) values ($user_id, $username)".update.run.transact(xa).unsafeRunSync
              logger.info("Setup Username: " + username + " User Id: " + user_id)
              reply("User name setup")
            }
        }
    }

    onCommand("set_channel") { implicit msg =>
      adminOrElse {
        admin => {
          withArgs {
            case Seq($.Long(id)) =>
              target_channel_id = id
              logger.info("Target channel set to id: " + target_channel_id.toString)
            case _ =>
              reply("Invalid argumentヽ(ಠ_ಠ)ノ, use /set_channel channel_id")
          }
        }
      } {
        user => reply("Admin user only")
      }
    }

    onChannelPost { implicit msg =>
      logger.info("Received message in channel: " + msg.text.orNull)
      if (msg.text.isDefined) {
        val text = msg.text.mkString
        //TODO Split post into names
        //TODO Search for names in db, get user_id
        //TODO Send Notification msg to user
        val nameRegex = "([А-Яа-я]+) ([А-Яа-я].)([А-Яа-я].)?,".r

        // get names
        val names = text.split("\n").map {
          case nameRegex(first, second, third) => logger.info("<" + first + "> <" + second + "> <" + third + ">")
            Some(("'" + first + " " + second.charAt(0) + "'").toLowerCase)
          case _ => None
        }
          .filter(_.isDefined)
          .map(_.get)

        for (name <- names) {
          findUserByNameWithDistance(name, 1)
            .foreach(user => {
              logger.info("Found user with name <" + name + ">, actual name = <" + user._1  + ">, id = <" + user._2 + ">")
              notifyUser(text, user)
            })
        }
      }
    }

    def findUserByNameWithDistance(name: String, distance: Long): Option[(Long, String)] = {
      sql"SELECT id, name FROM users WHERE levenshtein(name, $name) <= $distance ORDER BY levenshtein(name, $name) LIMIT 1"
        .query[(Long, String)]
        .to[List]
        .transact(xa)
        .unsafeRunSync()
        .headOption

    }

    def notifyUser(text: String, user: (Long, String)): Unit = {
      val (id, name) = user
      logger.info("Notifying user (id = <" + id + ">, name = <" + name + ">)")
      request(SendMessage(ChatId(id), text.replaceAll(Pattern.quote("(?i("+name+".+))\n"), "**$0**")))
    }
  }

  RandomBot.run()
}
