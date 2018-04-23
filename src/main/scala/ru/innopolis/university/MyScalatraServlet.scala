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
      "org.postgresql.Driver", "jdbc:postgresql:postgres", "postgres", ""
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

    onCommand("start") { implicit msg => reply("Hello! Never wonder what's up with your parcel again and use our bot ^^\n Type /help for more info") } //TODO Good start msg
    onCommand("help") { implicit msg => reply(
      """
        |/start - Start the work with the bot and display the greeting
        |/help - Show this help message
        |/setup Surname N. - Add "Surname N." to a list of names you follow
        |
        |Once you have set at least one name, you will start getting messages related to names you follow
        |Once the notification containing any of names you follow is in the post channel, you are going to be notified here.
        |This way, you will never miss the time you get a parcel.
      """.stripMargin) } //TODO Good help message
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
      if (!msg.text.isEmpty) {
        val text = msg.text.mkString
        //TODO Split post into names
        //TODO Search for names in db, get user_id
        //TODO Send Notification msg to user
        val nameRegex = "([А-Яа-я]+) ([А-Яа-я].)([А-Яа-я].)?,".r
        val names = text.split("\n").map(
          line => line match {
            case nameRegex(first, second, third) => logger.info("<" + first + "> <" + second + "> <" + third + ">")
              ("name = '" + first + " " + second.charAt(0) + "'").toLowerCase
            case _ => ()
          }).filter(_ != ()).mkString(" or ") match {
          case "" => "false";
          case n => n
        }
        logger.info(names.toString())
        var users = (sql"select id, name from users where " ++ Fragment.const(names)).query[(Long, String)].to[List].transact(xa).unsafeRunSync
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