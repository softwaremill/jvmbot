package com.softwaremill.jvmbot.eval

import javax.script.ScriptEngineManager

object NodeEval extends App {
  // create a script engine manager
  val factory = new ScriptEngineManager()
  // create a Nashorn script engine
  val engine = factory.getEngineByName("nashorn")
  // evaluate JavaScript statement
  val result = engine.eval(args.mkString(" "))
  println(result)
}
