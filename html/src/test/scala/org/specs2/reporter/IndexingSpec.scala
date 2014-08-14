package org.specs2.reporter

import org.specs2.data.Fold._
import org.specs2.io.FileSystem
import org.specs2.matcher.ControlMatchers._
import org.specs2.reporter.Indexing._
import org.specs2.{Specification, reporter}

import scalaz.Scalaz._
import scalaz._
import scalaz.stream._

class IndexingSpec extends Specification { def is = s2"""
 From the set of all the generated html pages we can generate an index and convert it to the tipue search format.

 An index is built from Html pages     $e1
 The index can be saved to a Json file $e2

"""

  def e1 = indexState(pages(0), reporter.Index.empty) must_== reporter.Index(Vector(IndexEntry(title = "page 1", text = "content1", tags = Vector(), path = "page1")))

  def e2 = {
    val path = "target/test/IndexingSpec/index.js"
    runFold(Process.emitAll(pages), indexFold(path)).run

    val expected =
    s"""|var tipuesearch = {"pages": [{"title":"page 1", "text":"content1", "tags":[], "loc":"page1"},
        |{"title":"page 2", "text":"content2", "tags":[], "loc":"page2"}]}""".stripMargin

    FileSystem.readFile(path).map(_.trim) must beOk(===(expected))
  }

  val pages = Seq(IndexedPage("page1", "page 1", "content1"),
                  IndexedPage("page2", "page 2", "content2"))

}

