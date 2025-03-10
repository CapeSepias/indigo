package indigo.shared.datatypes

import indigo.shared.time.Seconds

class RadiansTests extends munit.FunSuite {

  test("constants are equivalent") {
    assertEquals(Radians.TAU, Radians.`2PI`)
    assertEquals(Radians.TAUby2, Radians.PI)
    assertEquals(Radians.TAUby4, Radians.PIby2)
  }

  test("Can make a Radians instance from degrees") {

    assert(Radians.fromDegrees(0) ~== Radians.zero)
    assert(Radians.fromDegrees(180) ~== Radians.PI)
    assert(clue(Radians.fromDegrees(359)) ~== clue(clue(Radians.TAU) - Radians(0.0175d)))
    assert(Radians.fromDegrees(360) ~== Radians.zero)

  }

  test("Can convert seconds to Radians") {
    assert(Radians.fromSeconds(Seconds(0)) ~== Radians.zero)
    assert(Radians.fromSeconds(Seconds(0.5)) ~== Radians.PI)
    assert(Radians.fromSeconds(Seconds(1)) ~== Radians.zero)
    assert(Radians.fromSeconds(Seconds(1.5)) ~== Radians.PI)
  }

  test("Wrap Radians") {
    assert(Radians(0.0).wrap ~== Radians(0.0))
    assert(Radians(0.1).wrap ~== Radians(0.1))
    assert(Radians(-0.1).wrap ~== Radians.TAU - Radians(0.1))
    assert((Radians.TAU + Radians.TAUby4).wrap ~== Radians.TAUby4)
    assert((Radians.TAU - Radians.TAUby4).wrap ~== Radians.TAUby4 * Radians(3))
  }

}
