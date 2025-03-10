package indigo.shared

import indigo.shared.events.GlobalEvent

import scala.annotation.tailrec
import scala.util.control.NonFatal

/** An `Outcome` represents the result of some part of a frame update. It contains a value or an error (exception), and
  * optionally a list of events to be processed on the next frame.
  */
sealed trait Outcome[+A] derives CanEqual {

  def isResult: Boolean
  def isError: Boolean

  def unsafeGet: A
  def getOrElse[B >: A](b: => B): B
  def orElse[B >: A](b: => Outcome[B]): Outcome[B]

  def unsafeGlobalEvents: List[GlobalEvent]
  def globalEventsOrNil: List[GlobalEvent]

  def handleError[B >: A](recoverWith: Throwable => Outcome[B]): Outcome[B]

  def logCrash(reporter: PartialFunction[Throwable, String]): Outcome[A]

  def addGlobalEvents(newEvents: GlobalEvent*): Outcome[A]

  def addGlobalEvents(newEvents: => List[GlobalEvent]): Outcome[A]

  def createGlobalEvents(f: A => List[GlobalEvent]): Outcome[A]

  def clearGlobalEvents: Outcome[A]

  def replaceGlobalEvents(f: List[GlobalEvent] => List[GlobalEvent]): Outcome[A]

  def eventsAsOutcome: Outcome[List[GlobalEvent]]

  def mapAll[B](f: A => B, g: List[GlobalEvent] => List[GlobalEvent]): Outcome[B]

  def map[B](f: A => B): Outcome[B]

  def mapGlobalEvents(f: GlobalEvent => GlobalEvent): Outcome[A]

  def ap[B](of: Outcome[A => B]): Outcome[B]

  def merge[B, C](other: Outcome[B])(f: (A, B) => C): Outcome[C]

  def combine[B](other: Outcome[B]): Outcome[(A, B)]

  def flatMap[B](f: A => Outcome[B]): Outcome[B]
}
object Outcome {

  final case class Result[+A](state: A, globalEvents: List[GlobalEvent]) extends Outcome[A] {

    def isResult: Boolean = true
    def isError: Boolean  = false

    def unsafeGet: A =
      state
    def getOrElse[B >: A](b: => B): B =
      state
    def orElse[B >: A](b: => Outcome[B]): Outcome[B] =
      this

    def unsafeGlobalEvents: List[GlobalEvent] =
      globalEvents
    def globalEventsOrNil: List[GlobalEvent] =
      globalEvents

    def handleError[B >: A](recoverWith: Throwable => Outcome[B]): Outcome[B] =
      this

    def logCrash(reporter: PartialFunction[Throwable, String]): Outcome[A] =
      this

    def addGlobalEvents(newEvents: GlobalEvent*): Outcome[A] =
      addGlobalEvents(newEvents.toList)

    def addGlobalEvents(newEvents: => List[GlobalEvent]): Outcome[A] =
      Outcome(state, globalEvents ++ newEvents)

    def createGlobalEvents(f: A => List[GlobalEvent]): Outcome[A] =
      Outcome(state, globalEvents ++ f(state))

    def clearGlobalEvents: Outcome[A] =
      Outcome(state)

    def replaceGlobalEvents(f: List[GlobalEvent] => List[GlobalEvent]): Outcome[A] =
      Outcome(state, f(globalEvents))

    def eventsAsOutcome: Outcome[List[GlobalEvent]] =
      Outcome(globalEvents)

    def mapAll[B](f: A => B, g: List[GlobalEvent] => List[GlobalEvent]): Outcome[B] =
      Outcome(f(state), g(globalEvents))

    def map[B](f: A => B): Outcome[B] =
      Outcome(f(state), globalEvents)

    def mapGlobalEvents(f: GlobalEvent => GlobalEvent): Outcome[A] =
      Outcome(state, globalEvents.map(f))

    def ap[B](of: Outcome[A => B]): Outcome[B] =
      of match {
        case Error(e, r) =>
          Error(e, r)

        case Result(s, es) =>
          map(s).addGlobalEvents(es)
      }

    def merge[B, C](other: Outcome[B])(f: (A, B) => C): Outcome[C] =
      flatMap(a => other.map(b => (a, b))).map(p => f(p._1, p._2))

    def combine[B](other: Outcome[B]): Outcome[(A, B)] =
      other match {
        case Error(e, r) =>
          Error(e, r)

        case Result(s, es) =>
          Outcome((state, s), globalEvents ++ es)
      }

    def flatMap[B](f: A => Outcome[B]): Outcome[B] =
      f(state) match {
        case Error(e, r) =>
          Error(e, r)

        case Result(s, es) =>
          Outcome(s, globalEvents ++ es)
      }

  }

  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  final case class Error(e: Throwable, crashReporter: PartialFunction[Throwable, String]) extends Outcome[Nothing] {

    def isResult: Boolean = false
    def isError: Boolean  = true

    def unsafeGet: Nothing =
      throw e
    def getOrElse[B >: Nothing](b: => B): B =
      b
    def orElse[B >: Nothing](b: => Outcome[B]): Outcome[B] =
      b

    def unsafeGlobalEvents: List[GlobalEvent] =
      throw e
    def globalEventsOrNil: List[GlobalEvent] =
      Nil

    def handleError[B >: Nothing](recoverWith: Throwable => Outcome[B]): Outcome[B] =
      recoverWith(e)

    def logCrash(reporter: PartialFunction[Throwable, String]): Outcome[Nothing] =
      this.copy(crashReporter = reporter)

    def reportCrash: String =
      crashReporter.orElse[Throwable, String] { case (e: Throwable) =>
        e.getMessage + "\n" + e.getStackTrace.mkString("\n")
      }(e)

    def addGlobalEvents(newEvents: GlobalEvent*): Error                              = this
    def addGlobalEvents(newEvents: => List[GlobalEvent]): Error                      = this
    def createGlobalEvents(f: Nothing => List[GlobalEvent]): Error                   = this
    def clearGlobalEvents: Error                                                     = this
    def replaceGlobalEvents(f: List[GlobalEvent] => List[GlobalEvent]): Error        = this
    def eventsAsOutcome: Outcome[List[GlobalEvent]]                                  = this
    def mapAll[B](f: Nothing => B, g: List[GlobalEvent] => List[GlobalEvent]): Error = this
    def map[B](f: Nothing => B): Error                                               = this
    def mapGlobalEvents(f: GlobalEvent => GlobalEvent): Error                        = this
    def ap[B](of: Outcome[Nothing => B]): Outcome[B]                                 = this
    def merge[B, C](other: Outcome[B])(f: (Nothing, B) => C): Error                  = this
    def combine[B](other: Outcome[B]): Error                                         = this
    def flatMap[B](f: Nothing => Outcome[B]): Error                                  = this

  }

  object Error {
    def apply(e: Throwable): Error =
      Error(e, { case (ee: Throwable) => ee.getMessage })
  }

  implicit class ListWithOutcomeSequence[A](private val l: List[Outcome[A]]) extends AnyVal {
    def sequence: Outcome[List[A]] =
      Outcome.sequence(l)
  }
  implicit class tuple2Outcomes[A, B](private val t: (Outcome[A], Outcome[B])) extends AnyVal {
    def combine: Outcome[(A, B)] =
      t._1.combine(t._2)
    def merge[C](f: (A, B) => C): Outcome[C] =
      t._1.merge(t._2)(f)
    def map2[C](f: (A, B) => C): Outcome[C] =
      merge(f)
  }
  implicit class tuple3Outcomes[A, B, C](private val t: (Outcome[A], Outcome[B], Outcome[C])) extends AnyVal {
    def combine: Outcome[(A, B, C)] =
      t match {
        case (Result(s1, es1), Result(s2, es2), Result(s3, es3)) =>
          Outcome((s1, s2, s3), es1 ++ es2 ++ es3)

        case (Error(e, r), _, _) =>
          Error(e, r)

        case (_, Error(e, r), _) =>
          Error(e, r)

        case (_, _, Error(e, r)) =>
          Error(e, r)
      }
    def merge[D](f: (A, B, C) => D): Outcome[D] =
      for {
        aa <- t._1
        bb <- t._2
        cc <- t._3
      } yield f(aa, bb, cc)
    def map3[D](f: (A, B, C) => D): Outcome[D] =
      merge(f)
  }

  def apply[A](state: => A): Outcome[A] =
    try Outcome.Result[A](state, Nil)
    catch {
      case NonFatal(e) =>
        Outcome.Error(e)
    }

  def apply[A](state: => A, globalEvents: => List[GlobalEvent]): Outcome[A] =
    try Outcome.Result[A](state, globalEvents)
    catch {
      case NonFatal(e) =>
        Outcome.Error(e)
    }

  def unapply[A](outcome: Outcome[A]): Option[(A, List[GlobalEvent])] =
    outcome match {
      case Outcome.Error(_, _) =>
        None

      case Outcome.Result(s, es) =>
        Some((s, es))
    }

  def raiseError(throwable: Throwable): Outcome.Error =
    Outcome.Error(throwable)

  def sequence[A](l: List[Outcome[A]]): Outcome[List[A]] = {
    given CanEqual[Outcome[A], Outcome[A]] = CanEqual.derived

    @tailrec
    def rec(remaining: List[Outcome[A]], accA: List[A], accEvents: List[GlobalEvent]): Outcome[List[A]] =
      remaining match {
        case Nil =>
          Outcome(accA).addGlobalEvents(accEvents)

        case Error(e, r) :: _ =>
          Error(e, r)

        case Result(s, es) :: xs =>
          rec(xs, accA ++ List(s), accEvents ++ es)
      }

    rec(l, Nil, Nil)
  }

  def merge[A, B, C](oa: Outcome[A], ob: Outcome[B])(f: (A, B) => C): Outcome[C] =
    oa.merge(ob)(f)
  def map2[A, B, C](oa: Outcome[A], ob: Outcome[B])(f: (A, B) => C): Outcome[C] =
    merge(oa, ob)(f)
  def merge3[A, B, C, D](oa: Outcome[A], ob: Outcome[B], oc: Outcome[C])(f: (A, B, C) => D): Outcome[D] =
    for {
      aa <- oa
      bb <- ob
      cc <- oc
    } yield f(aa, bb, cc)
  def map3[A, B, C, D](oa: Outcome[A], ob: Outcome[B], oc: Outcome[C])(f: (A, B, C) => D): Outcome[D] =
    merge3(oa, ob, oc)(f)

  def combine[A, B](oa: Outcome[A], ob: Outcome[B]): Outcome[(A, B)] =
    oa.combine(ob)
  def combine3[A, B, C](oa: Outcome[A], ob: Outcome[B], oc: Outcome[C]): Outcome[(A, B, C)] =
    (oa, ob, oc) match {
      case (Result(s1, es1), Result(s2, es2), Result(s3, es3)) =>
        Outcome((s1, s2, s3), es1 ++ es2 ++ es3)

      case (Error(e, r), _, _) =>
        Error(e, r)

      case (_, Error(e, r), _) =>
        Error(e, r)

      case (_, _, Error(e, r)) =>
        Error(e, r)
    }

  def join[A](faa: Outcome[Outcome[A]]): Outcome[A] =
    faa match {
      case Error(e, r) =>
        Error(e, r)

      case Result(outcome, es) =>
        Outcome(outcome.unsafeGet, es ++ outcome.unsafeGlobalEvents)
    }
  def flatten[A](faa: Outcome[Outcome[A]]): Outcome[A] =
    join(faa)

}
