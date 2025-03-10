package com.example.sandbox

import indigo._

object SandboxView {

  val dudeCloneId: CloneId = CloneId("Dude")

  def updateView(model: SandboxGameModel, viewModel: SandboxViewModel, inputState: InputState): SceneUpdateFragment = {
    inputState.mouse.mouseClickAt match {
      case Some(position) => println("Mouse clicked at: " + position.toString())
      case None           => ()
    }

    SceneUpdateFragment.empty
      .addLayer(
        Layer(
          gameLayer(model, viewModel) ++ uiLayer(inputState)
        )
          .withDepth(Depth(300))
        // .withBlend(Blend.Alpha)
      )
      .addLayer(
        if (viewModel.useLightingLayer)
          Layer(lightingLayer(inputState))
            .withDepth(Depth(301))
            .withBlending(Blending.Lighting(RGBA.White.withAlpha(0.25)))
        else
          Layer.empty
      )
      // .addLayer(Layer(uiLayer(inputState)))
      .addCloneBlanks(CloneBlank(dudeCloneId, model.dude.dude.sprite))
    // .withSaturationLevel(0.5)
    // .withTint(RGBA.Cyan.withAmount(0.25))
    // .withUiColorOverlay(RGBA.Black.withAmount(0.5))
    // .withGameColorOverlay(RGBA.Red.withAmount(0.5))
  }

  def gameLayer(currentState: SandboxGameModel, viewModel: SandboxViewModel): List[SceneNode] =
    List(
      currentState.dude.walkDirection match {
        case d @ DudeLeft =>
          currentState.dude.dude.sprite
            .changeCycle(d.cycleName)
            .play()

        case d @ DudeRight =>
          currentState.dude.dude.sprite
            .changeCycle(d.cycleName)
            .play()

        case d @ DudeUp =>
          currentState.dude.dude.sprite
            .changeCycle(d.cycleName)
            .play()

        case d @ DudeDown =>
          currentState.dude.dude.sprite
            .changeCycle(d.cycleName)
            .play()

        case d @ DudeIdle =>
          currentState.dude.dude.sprite
            .changeCycle(d.cycleName)
            .play()
      },
      currentState.dude.dude.sprite
        .moveBy(8, 10)
        .moveBy(viewModel.offset)
        .modifyMaterial(
          _.withAlpha(1)
            .withTint(RGBA.Green.withAmount(0.25))
            .withSaturation(1.0)
        ),
      currentState.dude.dude.sprite
        .moveBy(8, -10)
        .modifyMaterial(_.withAlpha(0.5).withTint(RGBA.Red.withAmount(0.75))),
      CloneBatch(dudeCloneId, CloneBatchData(16, 64, Radians.zero, -1.0, 1.0))
    )

  def lightingLayer(inputState: InputState): List[SceneNode] =
    List(
      Graphic(114, 64 - 20, 320, 240, 1, SandboxAssets.lightMaterial.withTint(RGBA.Red))
        .withRef(Point(160, 120)),
      Graphic(114 - 20, 64 + 20, 320, 240, 1, SandboxAssets.lightMaterial.withTint(RGBA.Green))
        .withRef(Point(160, 120)),
      Graphic(114 + 20, 64 + 20, 320, 240, 1, SandboxAssets.lightMaterial.withTint(RGBA.Blue))
        .withRef(Point(160, 120)),
      Graphic(0, 0, 320, 240, 1, SandboxAssets.lightMaterial.withTint(RGBA(1, 1, 0.0, 1)).withAlpha(1))
        .withRef(Point(160, 120))
        .moveTo(inputState.mouse.position.x, inputState.mouse.position.y)
    )

  def uiLayer(inputState: InputState): List[SceneNode] =
    List(
      Text("AB!\n!C", 2, 2, 5, Fonts.fontKey, SandboxAssets.fontMaterial.withAlpha(0.5)).alignLeft,
      Text("AB!\n!C", 100, 2, 5, Fonts.fontKey, SandboxAssets.fontMaterial.withAlpha(0.5)).alignCenter,
      Text("AB!\n!C", 200, 2, 5, Fonts.fontKey, SandboxAssets.fontMaterial.withAlpha(0.5)).alignRight.onEvent {
        case (bounds, _: MouseEvent.Click) =>
          if (inputState.mouse.wasMouseClickedWithin(bounds))
            println("Hit me!")
          Nil

        case _ => Nil
      }
    )

}
