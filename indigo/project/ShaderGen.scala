import sbt._
import scala.sys.process._

object ShaderGen {

  val extensions: List[String] =
    List(".vert", ".frag")

  val fileFilter: String => Boolean =
    name => extensions.exists(e => name.endsWith(e))

  val tripleQuotes: String = "\"\"\""

  def template(
      name: String,
      vextexContents: String,
      fragmentContents: String
  ): String = {
    val vertexCode = {
      val withVertex =
        injectCode(
          vextexContents,
          "vertex",
          "vertexProgram",
          "void vertex(){}"
        )

      withVertex
    }

    val fragmentCode = {
      val withFragment =
        injectCode(
          fragmentContents,
          "fragment",
          "fragmentProgram",
          "void fragment(){}"
        )

      val withPrepare =
        injectCode(
          withFragment,
          "prepare",
          "prepareProgram",
          "void prepare(){}"
        )

      val withLight =
        injectCode(
          withPrepare,
          "light",
          "lightProgram",
          "void light(){}"
        )

      val withComposite =
        injectCode(
          withLight,
          "composite",
          "compositeProgram",
          "void composite(){}"
        )

      withComposite
    }

    val useNoWarn: Boolean =
      !(vertexCode.contains("vertexProgram.getOrElse") || fragmentCode.contains("fragmentProgram.getOrElse"))

    s"""package indigo.shaders
    |
    |import indigo.shared.shader.{RawShaderCode, ShaderId}
    |${if (useNoWarn) "import scala.annotation.nowarn" else ""}
    |
    |object $name extends RawShaderCode {
    |  val id: ShaderId = ShaderId("indigo_default_$name")
    |
    |  val vertex: String =
    |    vertexShader(None)
    |
    |  val fragment: String =
    |    fragmentShader(None, None, None, None)
    |
    |  ${if (useNoWarn) "@nowarn" else ""}
    |  def vertexShader(vertexProgram: Option[String]): String =
    |    s${tripleQuotes}${vertexCode}${tripleQuotes}
    |
    |  ${if (useNoWarn) "@nowarn" else ""}
    |  def fragmentShader(
    |    fragmentProgram: Option[String],
    |    prepareProgram: Option[String],
    |    lightProgram: Option[String],
    |    compositeProgram: Option[String]
    |  ): String =
    |    s${tripleQuotes}${fragmentCode}${tripleQuotes}
    |}
    """.stripMargin
  }

  def injectCode(program: String, programType: String, argName: String, default: String): String =
    if (program.contains(s"//#${programType}_start") && program.contains(s"//#${programType}_end")) {
      val code = program.split('\n').toList

      val start = code.takeWhile(line => !line.startsWith(s"//#${programType}_start"))
      val end   = code.reverse.takeWhile(line => !line.startsWith(s"//#${programType}_end")).reverse

      (start ++ List(s"""|$${$argName.getOrElse("$default")}""") ++ end).mkString("\n")
    } else program

  def splitAndPair(remaining: Seq[String], name: String, file: File): Option[ShaderDetails] =
    remaining match {
      case Nil =>
        None

      case ext :: exts if name.endsWith(ext) =>
        Some(ShaderDetails(name.substring(0, name.indexOf(ext)).capitalize, name, ext, IO.read(file)))

      case _ :: exts =>
        splitAndPair(exts, name, file)
    }

  def makeShader(files: Set[File], sourceManagedDir: File): Seq[File] = {
    println("Generating Indigo RawShaderCode Classes...")

    val shaderFiles: Seq[File] =
      files.filter(f => fileFilter(f.name)).toSeq

    val glslValidatorExitCode = {
      val command = Seq("glslangValidator", "-v")
      val run = sys.props("os.name").toLowerCase match {
        case x if x contains "windows" => Seq("cmd", "/C") ++ command
        case _ => command
      }
      run.!
    }

    println("***************")
    println("GLSL Validation")
    println("***************")

    if (glslValidatorExitCode == 0)
      shaderFiles.foreach { f =>
        val exit = {
          val command = Seq("glslangValidator", f.getCanonicalPath)
          val run = sys.props("os.name").toLowerCase match {
            case x if x contains "windows" => Seq("cmd", "/C") ++ command
            case _ => command
          }
          run.!
        }

        if (exit != 0)
          throw new Exception("GLSL Validation Error in: " + f.getName)
        else
          println(f.getName + " [valid]")
      }
    else
      println("**WARNING**: GLSL Validator not installed, shader code not checked.")

    val dict: Map[String, Seq[ShaderDetails]] =
      shaderFiles
        .map(f => splitAndPair(extensions, f.name, f))
        .collect { case Some(s) => s }
        .groupBy(_.newName)

    dict.toSeq.map {
      case (newName, subShaders: Seq[ShaderDetails]) if subShaders.length != 2 =>
        throw new Exception("RawShaderCode called '" + newName + "' did not appear to be a pair of shaders .vert and .frag")

      case (newName, subShaders: Seq[ShaderDetails]) if !subShaders.exists(_.ext == ".vert") =>
        throw new Exception("RawShaderCode called '" + newName + "' is missing a .vert shader")

      case (newName, subShaders: Seq[ShaderDetails]) if !subShaders.exists(_.ext == ".frag") =>
        throw new Exception("RawShaderCode called '" + newName + "' is missing a .frag shader")

      case (newName, subShaders: Seq[ShaderDetails]) =>
        val vert = subShaders.find(_.ext == ".vert").map(_.shaderCode)
        val frag = subShaders.find(_.ext == ".frag").map(_.shaderCode)

        val originalName: String =
          subShaders.headOption.map(_.originalName).getOrElse("<missing name... bad news.>")

        (vert, frag) match {
          case (Some(v), Some(f)) =>
            println("> " + originalName + " --> " + newName + ".scala")

            val file: File =
              sourceManagedDir / "indigo" / "shaders" / (newName + ".scala")

            val newContents: String =
              template(newName, v, f)

            IO.write(file, newContents)

            println("Written: " + file.getCanonicalPath)

            file

          case (None, _) =>
            throw new Exception("Couldn't find vertex shader details")

          case (_, None) =>
            throw new Exception("Couldn't find fragment shader details")

          case _ =>
            throw new Exception("Couldn't find shader details for reasons that are unclear...")
        }

    }
  }

  case class ShaderDetails(newName: String, originalName: String, ext: String, shaderCode: String)

}
