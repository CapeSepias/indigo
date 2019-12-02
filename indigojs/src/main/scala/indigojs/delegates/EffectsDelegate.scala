package indigojs.delegates

import indigo.shared.datatypes.Effects
import scala.scalajs.js.annotation._

@JSExportTopLevel("Effects")
final class EffectsDelegate(val alpha: Double, val tint: TintDelegate, val flip: FlipDelegate) {

  @JSExport
  def withAlpha(newAlpha: Double): EffectsDelegate =
    new EffectsDelegate(newAlpha, tint, flip)

  @JSExport
  def withTint(newTint: TintDelegate): EffectsDelegate =
    new EffectsDelegate(alpha, newTint, flip)

  @JSExport
  def withFlip(newFlip: FlipDelegate): EffectsDelegate =
    new EffectsDelegate(alpha, tint, newFlip)

  def toInternal: Effects =
    Effects(alpha, tint.toInternal, flip.toInternal)
}
