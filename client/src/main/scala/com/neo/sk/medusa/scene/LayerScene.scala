package com.neo.sk.medusa.scene

import com.neo.sk.medusa.common.AppSettings
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontPosture, FontWeight, Text}
import javafx.scene.{Group, Scene}

import com.neo.sk.medusa.ClientBoot
import com.neo.sk.medusa.common.AppSettings._
import com.neo.sk.medusa.controller.GameController


/**
  * User: gaohan
  * Date: 2018/12/6
  * Time: 4:19 PM
  */

object LayerScene{
  trait LayerSceneListener {
    def onKeyPressed(e: KeyCode): Unit
  }

}
class LayerScene {
  import LayerScene._
  var layerSceneListener: LayerSceneListener = _
  val group = new Group()
  val scene = new Scene(group, 1600,600)

  val layerWidth = AppSettings.layerCanvasW
  val layerHeight = AppSettings.layerCanvasH

  val viewWidth = AppSettings.viewCanvasW
  val viewHeight = AppSettings.viewCanvasH

  val layerMapCanvas = new Canvas(layerWidth,layerHeight)
  val layerInfoCanvas = new Canvas(layerWidth,layerHeight)
  val layerBgCanvas = new Canvas(layerWidth,layerHeight)
  val layerAppleCanvas = new Canvas(layerWidth,layerHeight)
  val layerAllSnakesCanvas = new Canvas(layerWidth,layerHeight)
  val layerMySnakeCanvas = new Canvas(layerWidth,layerHeight)

  val viewCanvas = new Canvas(800,400)
  val canvasCtx: GraphicsContext = viewCanvas.getGraphicsContext2D

  layerBgCanvas.setLayoutX(800)
  layerBgCanvas.setLayoutY(0)
  layerInfoCanvas.setLayoutX(1210)
  layerInfoCanvas.setLayoutY(0)
  layerMySnakeCanvas.setLayoutX(800)
  layerMySnakeCanvas.setLayoutY(210)
  layerAllSnakesCanvas.setLayoutX(1210)
  layerAllSnakesCanvas.setLayoutY(210)
  layerMapCanvas.setLayoutX(800)
  layerMapCanvas.setLayoutY(420)
  layerAppleCanvas.setLayoutX(1210)
  layerAppleCanvas.setLayoutY(420)



  viewCanvas.setLayoutX(0)
  viewCanvas.setLayoutY(100)

  if(GameController.grid.myId=="" && AppSettings.isLayer){
    canvasCtx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,30))
    canvasCtx.setFill(Color.BLACK)
    canvasCtx.fillText(s"Waiting for bot to join!",layerWidth/2,layerHeight/2)
    canvasCtx.fillText(s"Please do not worry!",layerWidth/2+100,layerHeight/2+50)
    canvasCtx.fillText(s"If you are bored!",layerWidth/2+200,layerHeight/2+100)
    canvasCtx.fillText(s"请玩一会手机!",layerWidth/2+300,layerHeight/2+150)
  }
  group.getChildren.add(viewCanvas)

//  viewCanvas.requestFocus()
//  viewCanvas.setOnKeyPressed(event => layerSceneListener.onKeyPressed(event.getCode))

  group.getChildren.add(layerMapCanvas)
  group.getChildren.add(layerInfoCanvas)
  group.getChildren.add(layerBgCanvas)
  group.getChildren.add(layerAllSnakesCanvas)
  group.getChildren.add(layerAppleCanvas)
  group.getChildren.add(layerMySnakeCanvas)

  layerMySnakeCanvas.requestFocus()
  layerMySnakeCanvas.setOnKeyPressed(event => layerSceneListener.onKeyPressed(event.getCode))



  def setLayerSceneListener(listener: LayerSceneListener): Unit = {
    layerSceneListener = listener
  }

}
