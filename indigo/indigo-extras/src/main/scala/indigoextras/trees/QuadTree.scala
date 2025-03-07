package indigoextras.trees

import indigoextras.geometry.BoundingBox
import indigoextras.geometry.LineSegment
import indigoextras.geometry.Vertex

import scala.annotation.tailrec

sealed trait QuadTree[T] derives CanEqual:
  val bounds: BoundingBox
  def isEmpty: Boolean

  def fetchElementAt(vertex: Vertex)(using CanEqual[T, T]): Option[T] =
    QuadTree.fetchElementAt(this, vertex)

  def insertElement(element: T, vertex: Vertex): QuadTree[T] =
    QuadTree.insertElementAt(vertex, this, element)

  def insertElements(elements: (T, Vertex)*): QuadTree[T] =
    insertElements(elements.toList)
  def insertElements(elements: List[(T, Vertex)]): QuadTree[T] =
    elements.foldLeft(this)((acc, item) => acc.insertElement(item._1, item._2))

  def removeElement(vertex: Vertex): QuadTree[T] =
    QuadTree.removeElement(this, vertex)

  @deprecated("use `toList` or `toListWithPosition` instead.")
  def asElementList(using CanEqual[T, T]): List[T] =
    QuadTree.asElementList(this)
  def toList(using CanEqual[T, T]): List[T] =
    QuadTree.toList(this)
  def toListWithPosition(using CanEqual[T, T]): List[(Vertex, T)] =
    QuadTree.toListWithPosition(this)

  def prune: QuadTree[T] =
    QuadTree.prune(this)

  @deprecated("use `findClosestTo` or `findClosestToWithPosition` instead")
  def searchByPoint(point: Vertex)(using CanEqual[T, T]): Option[T] =
    QuadTree.findClosestTo(this, point)
  def findClosestTo(vertex: Vertex)(using CanEqual[T, T]): Option[T] =
    QuadTree.findClosestTo(this, vertex)
  def findClosestToWithPosition(vertex: Vertex)(using CanEqual[T, T]): Option[(Vertex, T)] =
    QuadTree.findClosestToWithPosition(this, vertex)

  def searchByLine(start: Vertex, end: Vertex)(using CanEqual[T, T]): List[T] =
    QuadTree.searchByLine(this, start, end)
  def searchByLine(line: LineSegment)(using CanEqual[T, T]): List[T] =
    QuadTree.searchByLine(this, line)

  def searchByLineWithPosition(start: Vertex, end: Vertex)(using CanEqual[T, T]): List[(Vertex, T)] =
    QuadTree.searchByLineWithPosition(this, start, end)
  def searchByLineWithPosition(line: LineSegment)(using CanEqual[T, T]): List[(Vertex, T)] =
    QuadTree.searchByLineWithPosition(this, line)

  @deprecated("use `searchByBoundingBox` or `searchByBoundingBoxWithPosition` instead")
  def searchByRectangle(boundingBox: BoundingBox)(using CanEqual[T, T]): List[T] =
    QuadTree.searchByBoundingBox(this, boundingBox)
  def searchByBoundingBox(boundingBox: BoundingBox)(using CanEqual[T, T]): List[T] =
    QuadTree.searchByBoundingBox(this, boundingBox)
  def searchByBoundingBoxWithPosition(boundingBox: BoundingBox)(using CanEqual[T, T]): List[(Vertex, T)] =
    QuadTree.searchByBoundingBoxWithPosition(this, boundingBox)

  def prettyPrint: String =
    // Not tail recursive
    def rec(quadTree: QuadTree[T], indent: String): String =
      quadTree match
        case QuadTree.QuadEmpty(bounds) =>
          indent + s"Empty [${bounds.toString()}]"

        case QuadTree.QuadLeaf(bounds, exactPosition, value) =>
          indent + s"Leaf [${bounds.toString()}] - ${exactPosition.toString} - ${value.toString()}"

        case QuadTree.QuadBranch(bounds, a, b, c, d) =>
          s"""${indent}Branch [${bounds.toString()}]
             |${rec(a, indent + "  ")}
             |${rec(b, indent + "  ")}
             |${rec(c, indent + "  ")}
             |${rec(d, indent + "  ")}""".stripMargin

    rec(this, "")

  def ===(other: QuadTree[T])(using CanEqual[T, T]): Boolean =
    @tailrec
    def rec(a: List[QuadTree[T]], b: List[QuadTree[T]]): Boolean =
      (a, b) match
        case (Nil, Nil) =>
          true

        case (Nil, _) =>
          false

        case (_, Nil) =>
          false

        case (QuadTree.QuadEmpty(b1) :: as, QuadTree.QuadEmpty(b2) :: bs) if b1 ~== b2 =>
          rec(as, bs)

        case (QuadTree.QuadLeaf(b1, p1, v1) :: as, QuadTree.QuadLeaf(b2, p2, v2) :: bs)
            if (b1 ~== b2) && (p1 ~== p2) && v1 == v2 =>
          rec(as, bs)

        case (QuadTree.QuadBranch(bounds1, a1, b1, c1, d1) :: as, QuadTree.QuadBranch(bounds2, a2, b2, c2, d2) :: bs)
            if bounds1 ~== bounds2 =>
          rec(a1 :: b1 :: c1 :: d1 :: as, a2 :: b2 :: c2 :: d2 :: bs)

        case _ =>
          false

    rec(List(this), List(other))

  def !==(other: QuadTree[T])(using CanEqual[T, T]): Boolean =
    !(this === other)

object QuadTree:

  given [T](using CanEqual[T, T]): CanEqual[Option[QuadTree[T]], Option[QuadTree[T]]] = CanEqual.derived
  given [T](using CanEqual[T, T]): CanEqual[List[QuadTree[T]], List[QuadTree[T]]]     = CanEqual.derived

  def empty[T](width: Double, height: Double): QuadTree[T] =
    QuadEmpty(BoundingBox(0, 0, width, height))

  def empty[T](gridSize: Vertex): QuadTree[T] =
    QuadEmpty(BoundingBox(Vertex.zero, gridSize))

  def apply[T](elements: (T, Vertex)*): QuadTree[T] =
    QuadTree(elements.toList)
  def apply[T](elements: List[(T, Vertex)]): QuadTree[T] =
    QuadEmpty(BoundingBox.fromVertexCloud(elements.map(_._2))).insertElements(elements)

  final case class QuadBranch[T](bounds: BoundingBox, a: QuadTree[T], b: QuadTree[T], c: QuadTree[T], d: QuadTree[T])
      extends QuadTree[T]:
    def isEmpty: Boolean =
      a.isEmpty && b.isEmpty && c.isEmpty && d.isEmpty

  final case class QuadLeaf[T](bounds: BoundingBox, exactPosition: Vertex, value: T) extends QuadTree[T]:
    def isEmpty: Boolean = false

  final case class QuadEmpty[T](bounds: BoundingBox) extends QuadTree[T]:
    def isEmpty: Boolean = true

  object QuadBranch:

    def fromBounds[T](bounds: BoundingBox): QuadBranch[T] =
      fromBoundsAndQuads(bounds, subdivide(bounds))

    def subdivide(quadBounds: BoundingBox): (BoundingBox, BoundingBox, BoundingBox, BoundingBox) =
      val newWidth  = quadBounds.width / 2
      val newHeight = quadBounds.height / 2
      (
        BoundingBox(quadBounds.x, quadBounds.y, newWidth, newHeight),
        BoundingBox(
          quadBounds.x + newWidth,
          quadBounds.y,
          newWidth,
          newHeight
        ),
        BoundingBox(
          quadBounds.x,
          quadBounds.y + newHeight,
          newWidth,
          newHeight
        ),
        BoundingBox(
          quadBounds.x + newWidth,
          quadBounds.y + newHeight,
          newWidth,
          newHeight
        )
      )

    def fromBoundsAndQuads[T](
        bounds: BoundingBox,
        quads: (BoundingBox, BoundingBox, BoundingBox, BoundingBox)
    ): QuadBranch[T] =
      QuadBranch(
        bounds,
        QuadEmpty(quads._1),
        QuadEmpty(quads._2),
        QuadEmpty(quads._3),
        QuadEmpty(quads._4)
      )

  end QuadBranch

  def fetchElementAt[T](quadTree: QuadTree[T], vertex: Vertex)(using CanEqual[T, T]): Option[T] =
    @tailrec
    def rec(remaining: List[QuadTree[T]]): Option[T] =
      remaining match
        case Nil =>
          None

        case QuadLeaf(_, position, value) :: xs if position ~== vertex =>
          Some(value)

        case QuadBranch(bounds, a, b, c, d) :: xs if bounds.contains(vertex) =>
          rec(xs ++ List(a, b, c, d))

        case _ :: xs =>
          rec(xs)

    rec(List(quadTree))

  def insertElementAt[T](vertex: Vertex, quadTree: QuadTree[T], element: T): QuadTree[T] =
    quadTree match
      case QuadEmpty(bounds) if bounds.contains(vertex) =>
        // Straight insert
        QuadLeaf(bounds, vertex, element)

      case QuadLeaf(bounds, position, _) if position ~== vertex =>
        // Replace
        QuadLeaf(bounds, vertex, element)

      case QuadLeaf(bounds, position, value) if bounds.contains(vertex) =>
        // Both elements in the same region but not overlapping,
        // subdivide and insert both.
        QuadBranch
          .fromBounds(bounds)
          .insertElement(value, position) // original
          .insertElement(element, vertex) // new

      case QuadBranch(bounds, a, b, c, d) if bounds.contains(vertex) =>
        // Delegate to sub-regions
        QuadBranch[T](
          bounds,
          insertElementAt(vertex, a, element),
          insertElementAt(vertex, b, element),
          insertElementAt(vertex, c, element),
          insertElementAt(vertex, d, element)
        )

      case _ =>
        quadTree

  def removeElement[T](quadTree: QuadTree[T], vertex: Vertex): QuadTree[T] =
    quadTree match
      case QuadLeaf(bounds, p, _) if bounds.contains(vertex) && (p ~== vertex) =>
        QuadEmpty(bounds)

      case QuadBranch(bounds, a, b, c, d) if bounds.contains(vertex) =>
        QuadBranch[T](
          bounds,
          a.removeElement(vertex),
          b.removeElement(vertex),
          c.removeElement(vertex),
          d.removeElement(vertex)
        )

      case tree =>
        tree

  @deprecated("use `toList` or `toListWithPosition` instead.")
  def asElementList[T](quadTree: QuadTree[T])(using CanEqual[T, T]): List[T] =
    toList(quadTree)

  def toList[T](quadTree: QuadTree[T])(using CanEqual[T, T]): List[T] =
    @tailrec
    def rec(open: List[QuadTree[T]], acc: List[T]): List[T] =
      open match
        case Nil =>
          acc

        case x :: xs =>
          x match {
            case _: QuadEmpty[T] =>
              rec(xs, acc)

            case l: QuadLeaf[T] =>
              rec(xs, l.value :: acc)

            case b: QuadBranch[T] if b.isEmpty =>
              rec(xs, acc)

            case QuadBranch(_, a, b, c, d) =>
              val next =
                (if a.isEmpty then Nil else List(a)) ++
                  (if b.isEmpty then Nil else List(b)) ++
                  (if c.isEmpty then Nil else List(c)) ++
                  (if d.isEmpty then Nil else List(d))

              rec(xs ++ next, acc)
          }

    rec(List(quadTree), Nil)

  def toListWithPosition[T](quadTree: QuadTree[T])(using CanEqual[T, T]): List[(Vertex, T)] =
    @tailrec
    def rec(open: List[QuadTree[T]], acc: List[(Vertex, T)]): List[(Vertex, T)] =
      open match
        case Nil =>
          acc

        case x :: xs =>
          x match {
            case _: QuadEmpty[T] =>
              rec(xs, acc)

            case l: QuadLeaf[T] =>
              rec(xs, (l.exactPosition, l.value) :: acc)

            case b: QuadBranch[T] if b.isEmpty =>
              rec(xs, acc)

            case QuadBranch(_, a, b, c, d) =>
              val next =
                (if a.isEmpty then Nil else List(a)) ++
                  (if b.isEmpty then Nil else List(b)) ++
                  (if c.isEmpty then Nil else List(c)) ++
                  (if d.isEmpty then Nil else List(d))

              rec(xs ++ next, acc)
          }

    rec(List(quadTree), Nil)

  def prune[T](quadTree: QuadTree[T]): QuadTree[T] =
    quadTree match
      case l: QuadLeaf[T] =>
        l

      case e: QuadEmpty[T] =>
        e

      case b: QuadBranch[T] if b.isEmpty =>
        QuadEmpty(b.bounds)

      case QuadBranch(bounds, a, b, c, d) =>
        QuadBranch[T](bounds, a.prune, b.prune, c.prune, d.prune)

  @deprecated("use `findClosestTo` or `findClosestToWithPosition` instead")
  def searchByPoint[T](quadTree: QuadTree[T], vertex: Vertex)(using CanEqual[T, T]): Option[T] =
    findClosestTo(quadTree, vertex)
  def findClosestToWithPosition[T](quadTree: QuadTree[T], vertex: Vertex)(using CanEqual[T, T]): Option[(Vertex, T)] =
    @tailrec
    def rec(remaining: List[QuadTree[T]], closestDistance: Double, acc: Option[(Vertex, T)]): Option[(Vertex, T)] =
      remaining match
        case Nil =>
          acc

        case QuadLeaf(_, pos, value) :: rs if vertex.distanceTo(pos) < closestDistance =>
          rec(rs, vertex.distanceTo(pos), Some((pos, value)))

        case QuadBranch(bounds, a, b, c, d) :: rs if vertex.distanceTo(bounds.center) < closestDistance =>
          rec(a :: b :: c :: d :: rs, closestDistance, acc)

        case _ :: rs =>
          rec(rs, closestDistance, acc)

    rec(List(quadTree), Double.MaxValue, None)
  def findClosestTo[T](quadTree: QuadTree[T], vertex: Vertex)(using CanEqual[T, T]): Option[T] =
    findClosestToWithPosition(quadTree, vertex).map(_._2)

  def searchByLine[T](quadTree: QuadTree[T], start: Vertex, end: Vertex)(using CanEqual[T, T]): List[T] =
    searchByLine(quadTree, LineSegment(start, end))
  def searchByLineWithPosition[T](quadTree: QuadTree[T], start: Vertex, end: Vertex)(using
      CanEqual[T, T]
  ): List[(Vertex, T)] =
    searchByLineWithPosition(quadTree, LineSegment(start, end))

  def searchByLineWithPosition[T](quadTree: QuadTree[T], lineSegment: LineSegment)(using
      CanEqual[T, T]
  ): List[(Vertex, T)] =
    @tailrec
    def rec(remaining: List[QuadTree[T]], acc: List[(Vertex, T)]): List[(Vertex, T)] =
      remaining match
        case Nil =>
          acc

        case QuadBranch(bounds, a, b, c, d) :: rs if bounds.contains(lineSegment.start) =>
          rec(rs ++ List(a, b, c, d), acc)

        case QuadBranch(bounds, a, b, c, d) :: rs if bounds.contains(lineSegment.end) =>
          rec(rs ++ List(a, b, c, d), acc)

        case QuadBranch(bounds, a, b, c, d) :: rs if bounds.lineIntersects(lineSegment) =>
          rec(rs ++ List(a, b, c, d), acc)

        case QuadLeaf(bounds, pos, value) :: rs if bounds.contains(lineSegment.start) =>
          rec(rs, (pos, value) :: acc)

        case QuadLeaf(bounds, pos, value) :: rs if bounds.contains(lineSegment.end) =>
          rec(rs, (pos, value) :: acc)

        case QuadLeaf(bounds, pos, value) :: rs if bounds.lineIntersects(lineSegment) =>
          rec(rs, (pos, value) :: acc)

        case _ :: rs =>
          rec(rs, acc)

    rec(List(quadTree), Nil)

  def searchByLine[T](quadTree: QuadTree[T], lineSegment: LineSegment)(using CanEqual[T, T]): List[T] =
    searchByLineWithPosition(quadTree, lineSegment).map(_._2)

  def searchByBoundingBoxWithPosition[T](quadTree: QuadTree[T], boundingBox: BoundingBox)(using
      CanEqual[T, T]
  ): List[(Vertex, T)] =
    @tailrec
    def rec(remaining: List[QuadTree[T]], acc: List[(Vertex, T)]): List[(Vertex, T)] =
      remaining match
        case Nil =>
          acc

        case QuadLeaf(_, exactPosition, value) :: rs if boundingBox.contains(exactPosition) =>
          rec(rs, (exactPosition, value) :: acc)

        case QuadBranch(bounds, a, b, c, d) :: rs if boundingBox.overlaps(bounds) =>
          rec(rs ++ List(a, b, c, d), acc)

        case _ :: rs =>
          rec(rs, acc)

    rec(List(quadTree), Nil)

  def searchByBoundingBox[T](quadTree: QuadTree[T], boundingBox: BoundingBox)(using CanEqual[T, T]): List[T] =
    searchByBoundingBoxWithPosition(quadTree, boundingBox).map(_._2)

end QuadTree
