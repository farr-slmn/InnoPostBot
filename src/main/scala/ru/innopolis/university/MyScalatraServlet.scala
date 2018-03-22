package ru.innopolis.university

import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Extractors, Polling, TelegramBot, Webhook}
import org.scalatra._

import scala.util.Random


class MyScalatraServlet extends ScalatraServlet {

  //change Polling to Webhook and uncomment @port @webhookUrl after deployment
  object RandomBot extends TelegramBot with Polling with Commands {
    def token = ""

    //override val port = 8443
    //override val webhookUrl = "https://1d1ceb07.ngrok.io" // Mike, need your server webhook Url

    val rng = new Random(System.currentTimeMillis())
    onCommand("coin", "flip") { implicit msg => reply(if (rng.nextBoolean()) "Head!" else "Tail!") }
    onCommand("real") { implicit msg => reply(rng.nextDouble().toString) }
    onCommand("die") { implicit msg => reply((rng.nextInt(6) + 1).toString) }
    onCommand("dice") { implicit msg => reply((rng.nextInt(6) + 1) + " " + (rng.nextInt(6) + 1)) }
    onCommand("random", "rand") { implicit msg =>
      withArgs {
        case Seq(Extractors.Int(n)) if n > 0 =>
          reply(rng.nextInt(n).toString)
        case _ =>
          reply("Invalid argumentヽ(ಠ_ಠ)ノ")
      }
    }
    onCommand("/choose", "/pick") { implicit msg =>
      withArgs { args =>
        reply(if (args.isEmpty) "Empty list." else args(rng.nextInt(args.size)))
      }
    }
    onCommand("info") { implicit msg => reply("send dunes") }
  }

  RandomBot.run()
}