package ru.innopolis.university

import info.mukel.telegrambot4s.api.declarative.{Action, ChannelPosts, Commands}
import info.mukel.telegrambot4s.api.{Polling, Extractors => $, _}
import info.mukel.telegrambot4s.models.{ChatId, Message, User}
import info.mukel.telegrambot4s.methods.SendMessage

import com.redis._
import org.scalatra._
import scala.collection._

class MyScalatraServlet extends ScalatraServlet {

  //change Polling to Webhook and uncomment @port @webhookUrl after deployment
  object RandomBot extends TelegramBot with Polling with Commands with ChannelPosts {
    def token = "TOKEN" //TODO change to function

    private val redis = new RedisClient("localhost", 6379)

    redis.del("Петров П")
    /*
      scala> import com.redis._
      import com.redis._

      scala> val r = new RedisClient("localhost", 6379)
      r: com.redis.RedisClient = localhost:6379

      scala> r.set("key", "some value")
      res3: Boolean = true

      scala> r.get("key")
      res4: Option[String] = Some(some value)
     */

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

    onCommand("start")  { implicit msg => reply("send nudes" ) } //TODO Good start msg
    onCommand("help")   { implicit msg => reply("send nudes") }  //TODO Good help message
    onCommand("whoiam") { implicit msg => reply(msg.source.toString) }

    onCommand("setup") {
      implicit msg =>
        withArgs {
          args =>
            if (args.isEmpty) {
              reply("Invalid argumentヽ(ಠ_ಠ)ノ, use /setup Credential e.g. Петров П")
            } else {
              val username = args.mkString(" ")
              val user_id = msg.source
              //TODO Insert DB update here
              redis.sadd(username, user_id.toString)
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
      //TODO Split post into names
      //TODO Search for names in db, get user_id
      //TODO Send Notification msg to user

      redis.smembers("Петров П") match {
        case Some(user_ids) => logger.info("Петров найден нахуй: " + user_ids.mkString(" "))
        case None => logger.info("Петров не найден")
      }

      request(SendMessage(ChatId(212071762), msg.text.orNull))
    }
  }

  RandomBot.run()
}