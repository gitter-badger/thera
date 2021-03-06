package thera

import better.files._, File._
import org.apache.commons.io.IOUtils

import scala.util.matching.Regex

import cats._, cats.implicits._, cats.effect._, cats.data.{ EitherT => ET }
import io.circe._


object filter {
  type TemplateFilter = String => Ef[String]

  /**
   * Command line filter. Invokes an external application, obtains its
   * input and output streams and uses it as a filter. It feeds
   * the template's contents to the output stream and consumes its
   * input stream as the transformed template.
   */ 
  def command(cmd: String, executeFrom: File = file"_site", encoding: String = "utf8"): TemplateFilter = tml => {
    val proc = sys.runtime.exec(cmd, null, executeFrom.toJava)
    val is   = proc.getInputStream
    val os   = proc.getOutputStream
    val es   = proc.getErrorStream

    def closeAll(): Unit = {
      os.close()
      is.close()
      es.close()
    }

    (for {
      _   <- att { IOUtils.write(tml, os, encoding) }
      _   <- att { os.close() }
      res <- att { IOUtils.toString(is, encoding)}
      _   <- att { println(IOUtils.toString(es, encoding)) }
      _   <- att { closeAll() }
    } yield res).leftMap(e => { closeAll(); e })
  }
}