package ru.innopolis.university

import java.util.regex.Pattern

import cats.effect.IO
import doobie._
import doobie.implicits._
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
    lazy val token = scala.util.Properties
      .envOrNone("BOT_TOKEN")
      .getOrElse(Source.fromFile("bot.token").getLines().mkString)

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", "jdbc:postgresql:postgres", "postgres", "scalaproject"
    )
    val y = xa.yolo

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

    onCommand("start") { implicit msg => withArgs {
      args => {
        val user_id = msg.source
        sql"insert into recepients (uid) values ($user_id)".update.run.transact(xa).unsafeRunSync
        reply("Hello! Never wonder what's up with your parcel again and use our bot ^^\n Type /help for more info")
      }}
    }

    onCommand("help") { implicit msg => reply(
      """
        |/start - Start the work with the bot and display the greeting
        |/help - Show this help message
        |/setup Surname N - Add "Surname N" to a list of names you follow
        |
        |Once you have set at least one name, you will start getting messages related to names you follow
        |Once the notification containing any of names you follow is in the post channel, you are going to be notified here.
        |This way, you will never miss the time you get a parcel.
      """.stripMargin) }
    onCommand("whoiam") { implicit msg => reply(msg.source.toString) }

    onCommand("add") {
      implicit msg =>
        withArgs {
          args =>
            if (args.isEmpty) {
              reply("Invalid argumentヽ(ಠ_ಠ)ノ, use /setup Credential e.g. Петров И")
            } else {
              val subname = args.mkString(" ").toLowerCase
              val user_id = msg.source
              sql"insert into users (subname) values ($subname)".update.run.transact(xa).unsafeRunSync
              sql"insert into subscriptions (uid, subid) values ($user_id, $subname)".update.run.transact(xa).unsafeRunSync
              logger.info("User <" + subname + "> is followed\nUser Id: " + user_id)
              reply("User name is added")
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
        val nameRegex = "([А-Яа-я]+) ([А-Яа-я].)([А-Яа-я].)?,".r
        val names = text.split("\n").map{
          case nameRegex(first, second, third) => logger.info("<" + first + "> <" + second + "> <" + third + ">")
            ("name = '" + first + " " + second.charAt(0) + "'").toLowerCase
          case _ => ()
        }.filter(_ != ()).mkString(" or ") match {
          case "" => "false";
          case n => n
        }
        logger.info(names.toString)
        var users = (sql"select id, subname from subsriptions where " ++ Fragment.const(names)).query[(Long, String)].to[List].transact(xa).unsafeRunSync
        logger.info(users.getClass.toString)
        users.foreach(user => {
          val (id, name) = user
          logger.info("User id: <" + id + "> for name: <" + name + ">")
          request(SendMessage(ChatId(id), text.replaceAll(Pattern.quote("(?i("+name+".+))\n"), "**$0**")))
        })
      }
    }
  }

  RandomBot.run()
}
