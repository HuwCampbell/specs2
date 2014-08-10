package org.specs2.reporter

case class HtmlOptions(outDir: String, baseDir: String, template: String, variables: Map[String, String], noStats: Boolean) {
  def javascriptDir = outDir+"javascript/"
  def indexDir      = javascriptDir+"tipuesearch/"
  def indexFile     = indexDir+"tipuesearch_contents.js"
}

object HtmlOptions {
  val outDir = "target/specs2-reports/"
  def template(outDir: String) = outDir+"/templates/specs2.html"
  val variables = Map[String, String]()
  val baseDir = "."
  val noStats = false
}


