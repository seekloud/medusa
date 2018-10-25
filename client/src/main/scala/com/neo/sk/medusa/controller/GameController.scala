package com.neo.sk.medusa.controller

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.medusa.ClientBoot
import com.neo.sk.medusa.ClientBoot.gameMessageReceiver
import com.neo.sk.medusa.actor.GameMessageReceiver.GridInitial
import com.neo.sk.medusa.actor.WSClient
import com.neo.sk.medusa.common.StageContext
import com.neo.sk.medusa.scene.GameScene
import com.neo.sk.medusa.snake.{Boundary, Point, Protocol}

/**
	* Created by wangxicheng on 2018/10/25.
	*/
object GameController {
	val bounds = Point(Boundary.w, Boundary.h)
	val grid = new GridOnClient(bounds)
	val myId = ""
	var basicTime = 0l
	var myPorportion = 1.0
}

class GameController(id: String,
										 name: String,
										 accessCode: String,
										 stageCtx: StageContext,
										 wsClient: ActorRef[Protocol.WsSendMsg]) {
	
	import GameController._
	
	def connectToGameServer = {
		ClientBoot.addToPlatform {
      val gameScene = new GameScene()
			stageCtx.switchScene(gameScene.scene, "Gaming")
			gameMessageReceiver ! GridInitial(grid)

		}
	}
	
	
}
