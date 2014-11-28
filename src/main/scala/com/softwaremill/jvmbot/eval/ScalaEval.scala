package com.softwaremill.jvmbot.eval

object ScalaEval extends App {
  import scala.reflect.runtime.{universe => ru}
  import scala.tools.reflect.ToolBox

  val cm = ru.runtimeMirror(getClass.getClassLoader)
  val tb = cm.mkToolBox()
  println(tb.eval(tb.parse(args.mkString(" "))))
}
