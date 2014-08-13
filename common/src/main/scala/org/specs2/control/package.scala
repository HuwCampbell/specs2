package org.specs2

import scalaz.{Monad, Monoid}
import scalaz.std.anyVal._
import scalaz.effect._
import org.specs2.execute.{AsResult, Result}
import org.specs2.execute.Error.ThrowableException
import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.syntax.bind._

package object control {
  type Logger = String => IO[Unit]
  lazy val noLogging = (s: String) => IO(())
  lazy val consoleLogging = (s: String) => IO(println(s))

  type Action[A] = ActionT[IO, Unit, Logger, A]
  object Actions extends ActionTSupport[IO, Unit, Logger] {
    def unit: Action[Unit] = empty(implicitly[Monad[IO]], implicitly[Monoid[Unit]])
  }

  /** log a value, using the logger coming from the Reader environment */
  def log[R](r: R): Action[Unit] =
    Actions.ask.flatMap(logger => logger(r.toString).liftIO[Action])

  /** log a Throwable with its stacktrace and cause, using the logger coming from the Reader environment */
  def logThrowable(t: Throwable, verbose: Boolean): Action[Unit] =
    if (verbose) logThrowable(t) else Actions.unit

  def logThrowable(t: Throwable): Action[Unit] =
    log(t.getMessage) >>
    log(t.getStackTrace.mkString("\n")) >>
      (if (t.getCause != null) logThrowable(t.getCause)
       else                    Actions.unit)

  /** log a value, using the logger coming from the Reader environment, only if verbose is true */
  def log[R](r: R, verbose: Boolean): Action[Unit] =
    if (verbose) log(r) else Actions.unit

  /**
   * This implicit allows any IO[result] to be used inside an example:
   *
   * "this should work" in {
   *   IO(success)
   * }
   */
  implicit def ioResultAsResult[T : AsResult]: AsResult[IO[T]] = new AsResult[IO[T]] {
    def asResult(io: =>IO[T]) = AsResult(io.unsafePerformIO())
  }

  /**
   * This implicit allows an IOAction[result] to be used inside an example.
   *
   * For example to read a database.
   *
   */
  implicit def ioActionResultAsResult[T : AsResult]: AsResult[Action[T]] = new AsResult[Action[T]] {
    def asResult(ioAction: =>Action[T]): Result =
      ioAction.execute(noLogging).unsafePerformIO.foldAll(
        ok        => AsResult(ok),
        fail      => org.specs2.execute.Failure(fail),
        throwable => org.specs2.execute.Error(throwable),
        (m, t)    => org.specs2.execute.Error(m, new ThrowableException(t))
      )
  }

  implicit class ioActionToProcess[T](action: Action[T]) {
    def toProcess = Process(action.toTask).eval
  }

  implicit class ioActionToTask[T](action: Action[T]) {
    def toTask = Task.delay (
      action.execute(noLogging).unsafePerformIO).flatMap(_.fold(
      t => Task.now(t),
      error => error.fold(
        s => Task.fail(new Exception(s)),
        t => Task.fail(t),
        (s, t) => Task.fail(t)
      )))
  }

  implicit class ioActionToOption[T](action: Action[T]) {
    def runOption = action.toTask.attemptRun.toOption
  }

  implicit class taskToAction[T](task: Task[T]) {
    def toAction = Actions.fromTask(task)
  }

}

