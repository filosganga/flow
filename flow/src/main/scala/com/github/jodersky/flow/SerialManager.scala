package com.github.jodersky.flow

import java.io.IOException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.github.jodersky.flow.internal.InternalSerial

import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.SupervisorStrategy.Stop
import akka.actor.actorRef2Scala

/**
 * Actor that manages serial port creation. Once opened, a serial port is handed over to
 * a dedicated operator actor that acts as an intermediate between client code and the native system serial port.
 * @see SerialOperator
 */
class SerialManager extends Actor with ActorLogging {
  import SerialManager._
  import context._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: IOException => Stop
      case _: Exception => Escalate
    }

  def receive = {
    case c @ Open(s) => Try { InternalSerial.open(s.port, s.baud, s.characterSize, s.twoStopBits, s.parity.id) } match {
      case Failure(t) => sender ! CommandFailed(c, t)
      case Success(serial) => {
        val operator = context.actorOf(SerialOperator(serial), name = escapePortString(s.port))
        val settings = SerialSettings(serial.port, serial.baud, serial.characterSize, serial.twoStopBits, Parity(serial.parity)) 
        sender.tell(Opened(settings, operator), operator)
      }
    }
  }

}

object SerialManager {

  private def escapePortString(port: String) = port collect {
    case '/' => '-'
    case c => c
  }

}