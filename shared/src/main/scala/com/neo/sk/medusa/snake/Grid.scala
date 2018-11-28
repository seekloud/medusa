package com.neo.sk.medusa.snake

import java.awt.event.KeyEvent

import com.neo.sk.medusa.snake.Protocol.{square,fSpeed}

import scala.collection.mutable.ListBuffer
import scala.util.Random


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())

  val defaultLength = 5
  val appleNum = 25
  val appleLife = 500
  val historyRankLength = 5
  val basicSpeed = 10.0
  val speedUpRange = 50

  val freeFrameTime = 30
  var snakes = Map.empty[String, SnakeInfo]
  var snakes4client = Map.empty[String, Snake4Client]
  var frameCount = 0l
  var grid = Map[Point, Spot]()
  var actionMap = Map.empty[Long, Map[String, Int]]
  var deadSnakeList = List.empty[DeadSnakeInfo]
  var killMap = Map.empty[String, List[(String,String)]]

  def removeSnake(id: String) = {
    val r1 = snakes.get(id)
		val r2 = snakes4client.get(id)
    if (r1.isDefined) {
      snakes -= id
    }
		if(r2.isDefined) {
			snakes4client -= id
		}
  }


  def addAction(id: String, keyCode: Int) = {
    addActionWithFrame(id, keyCode, frameCount)
  }

  def addActionWithFrame(id: String, keyCode: Int, frame: Long) = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (frame -> tmp)
  }


  def update(isSynced: Boolean) = {
    if(!isSynced) {
      updateSnakes()
    }
    updateSpots(false)
    if(isSynced) {
      frameCount -= 1
    }
//    actionMap -= (frameCount - Protocol.advanceFrame)
    frameCount += 1
  }

  def countBody(): Unit
  
  def feedApple(appleCount: Int, appleType: Int, deadSnake: Option[String] = None): Unit

  def eatFood(snakeId: String, newHead: Point, newSpeedInit: Double, speedOrNotInit: Boolean): Option[(Int, Double, Boolean)]

//  def speedUp(snake: SnakeInfo, newDirection: Point): Option[(Boolean, Double)]

  private[this] def updateSpots(front: Boolean) = {
    var appleCount = 0
    grid = grid.filter { case (_, spot) =>
      spot match {
        case x@Apple(_, _, _, _) if x.frame >= frameCount => true
        case _ => false
      }
    }.map {
      case (p, a@Apple(_,  appleType, frame, targetAppleOpt)) =>
        if (appleType == FoodType.normal) {
          appleCount += 1
          (p, a)
        } else if (appleType == FoodType.intermediate && targetAppleOpt.nonEmpty) {
          val targetApple = targetAppleOpt.get
          if (p == targetApple._1) {
            val apple = Apple(targetApple._2, FoodType.deadBody, frame)
            (p, apple)
          } else {
            val nextLoc = p pathTo targetApple._1
            if (nextLoc.nonEmpty) {
              val apple = Apple(targetApple._2, FoodType.intermediate, frame, targetAppleOpt)
              (nextLoc.get, apple)
            } else {
              val apple = Apple(targetApple._2, FoodType.deadBody, frame)
              (p, apple)
            }
          }
        } else {
          (p, a)
        }

      case x => x
    }
    countBody()
    feedApple(appleCount, FoodType.normal)
  }

  def randomPoint():Point = {
    val randomArea = random.nextInt(3)
    val rPoint = randomArea match {
      case 0 =>
        Point(random.nextInt(Boundary.w -200)  + 100, random.nextInt(100)  + 100)
      case 1 =>
        Point(random.nextInt(100)  + 100, random.nextInt(Boundary.h -200)  + 100)
      case 2 =>
        Point(random.nextInt(100)  + Boundary.w -200, random.nextInt(Boundary.h -200)  + 100)
      case _ =>
        Point(random.nextInt(Boundary.w -200)  + 100, random.nextInt(100)  + Boundary.h -200)
    }

    rPoint
  }


  def randomHeadEmptyPoint(): Point = {
    var p = randomPoint()
    while (grid.contains(p)) {
      p = randomPoint()
    }
    p
  }

  def randomEmptyPoint(): Point = {
    var p = Point(random.nextInt(boundary.x - 2 * boundaryWidth) + boundaryWidth, random.nextInt(boundary.y - 2 * boundaryWidth) + boundaryWidth)
    while (grid.contains(p)) {
      p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
    }
    p
  }
  
  def getSafeDirection(p: Point) = {
    val down = (p.y, Point(0, 1))
    val up = (boundary.y - p.y, Point(0, -1))
    val right = (p.x, Point(1, 0))
    val left = (boundary.x - p.x, Point(-1, 0))
    List(down, up, right, left).minBy(_._1)._2
  }
  
  def updateSnakes():Unit
//  def updateASnake(snake: SnakeInfo, actMap: Map[String, Int]): Either[String, SnakeInfo]

  
  def getGridSyncData = {
    var appleDetails: List[Ap] = Nil
    grid.foreach {
      case (p, Apple(score, appleType, frame, targetAppleOpt)) => appleDetails ::= Ap(score, appleType, p.x, p.y, frame, targetAppleOpt)
      case _ =>
    }
    val snake4client = snakes.values.map{
      s => Snake4Client(s.id, s.name, s.head, s.tail, s.color, s.direction, s.joints, s.speed,s.length, s.extend)
    }
    Protocol.GridDataSync(
      frameCount,
      snake4client.toList,
      appleDetails,
      System.currentTimeMillis()
    )
  }
  def getGridSyncData4Client = {
    var appleDetails: List[Ap] = Nil
    grid.foreach {
      case (p, Apple(score, appleType, frame, targetAppleOpt)) => appleDetails ::= Ap(score, appleType, p.x, p.y, frame, targetAppleOpt)
      case _ =>
    }
    Protocol.GridDataSync(
      frameCount,
      snakes4client.values.toList,
      appleDetails
    )

  }

  def getGridSyncDataNoApp = {
    val snake4client = snakes.values.map{
      s => Snake4Client(s.id, s.name, s.head, s.tail, s.color, s.direction, s.joints, s.speed,s.length, s.extend)
    }
    Protocol.GridDataSyncNoApp(
      frameCount,
      snake4client.toList
    )
  }


}
