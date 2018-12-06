package com.neo.sk.medusa.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.neo.sk.medusa.protocol.PlayInfoProtocol.PlayerInfo
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.neo.sk.medusa.core.UserManager.{ChildDead, YourUserUnwatched, getUserActor}
import net.sf.ehcache.transaction.xa.commands.Command
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import com.neo.sk.medusa.Boot.watchManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks

object RoomManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val UserLeftRoomTime= 5.minutes

  private final val maxUserNum = 30

  private val idGenerator = new AtomicLong(1000001l)

  sealed trait Command

  final case class ChildDead[U](roomId: Long, childRef: ActorRef[U]) extends Command

  case class JoinGame(playerId: String, playerName: String, roomId: Long,isNewJoin:Boolean, userActor: ActorRef[UserActor.Command],pwd:Option[String]) extends Command

  case class UserLeftRoom(playerId: String, roomId: Long) extends Command

  case class RoomEmptyTimerKey(roomId:Long)extends Command

  case class RoomEmptyKill(roomId:Long)extends Command

  case class GetPlayerByRoomId(playerId:String, roomId:Long, watcherId:String, watcherRef:ActorRef[WatcherActor.Command]) extends Command

  case class GetRoomIdByPlayerId(playerId: String, replyTo:ActorRef[RoomIdRsp]) extends Command //接口请求 给平台roomid，记得之后改成String

  case class RoomIdRsp(roomId:Long)

  case class GetPlayerListReq(roomId:Long, replyTo:ActorRef[GetPlayerListRsp]) extends Command

  case class GetPlayerListRsp(playerList:List[PlayerInfo])

  case class GetRoomListReq(replyTo:ActorRef[GetRoomListRsp]) extends Command

  case class GetRoomListRsp(roomList:List[Long])

  case class ReStartGame(playerId:String,roomId:Long) extends Command

  case class YourUserUnwatched(playerId: String, watcherId: String,roomId:Long) extends Command

  case class INeedApple(playerId: String, watcherId: String,roomId:Long) extends Command

  case class CreateRoom(playerId: String, playerName: String, userActor: ActorRef[UserActor.Command],pwd:Option[String]) extends Command

  val behaviors: Behavior[Command] = {
    log.info(s"RoomManager start...")
    Behaviors.setup[Command] {
      _ =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            //roomId->userNum
            val roomNumMap = mutable.HashMap.empty[Long, Int]
            //userId->(roomId,userName)
            val userRoomMap = mutable.HashMap.empty[String, (Long, String)]
            val userRoomPwdMap = mutable.HashMap.empty[Long,(String,String)]   //房间-->[创建者，密码]
            idle(roomNumMap, userRoomMap,userRoomPwdMap)
        }
    }
  }


  import Breaks.{break, breakable}
	private def idle(roomNumMap: mutable.HashMap[Long, Int],
                   userRoomMap: mutable.HashMap[String, (Long, String)],
                   userRoomPwdMap:mutable.HashMap[Long, (String, String)])
                  (implicit timer: TimerScheduler[Command]) =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case JoinGame(playerId, playerName, roomId, isNewJoin, userActor,password) =>
            //分配房间 启动相应actor
            if (roomId == -1) {
              //未指定房间，带密码也没有意义，直接寻找房间人数未满且不带密码的
              val fewNumRoom = roomNumMap.filter(_._2<maxUserNum)//先选出房间人数未满的房间
              var randomRoomId = Long.MinValue
              if(!fewNumRoom.isEmpty){
                breakable{
                  for((k0,_)<- fewNumRoom){
                    breakable{
                      for ((k1, (_, v2)) <- userRoomPwdMap) {
                        if (k0 == k1 && v2 == "") {   //再在人数未满的房间寻找不带密码的
                          randomRoomId = k0
                          break()
                        }
                      }
                    }
                    if(randomRoomId!=Long.MinValue) break()
                  }
                }
              }
              if(randomRoomId !=Long.MinValue){
                timer.cancel(RoomEmptyTimerKey(randomRoomId))
                //新加入游戏的 roomNum加一 否则不变
                if(isNewJoin){
                  roomNumMap.update(randomRoomId, roomNumMap(randomRoomId) + 1)
                }
                userRoomMap.put(playerId, (randomRoomId, playerName))
                userActor ! UserActor.JoinRoomSuccess(randomRoomId, getRoomActor(ctx, randomRoomId))
              }else {         //如果房间人数全满  或者 存在人数未满但有密码的房间,那么就新建一个房间
                log.info(s"all room is full or you have not permissions to enter any room ,start a new room.. ")
                ctx.self ! CreateRoom(playerId,playerName,userActor,password)
              }
            } else {
              //指定房间号
              if (roomNumMap.get(roomId).nonEmpty && userRoomPwdMap(roomId)._2.equals(password.get)) {
                //房间已存在并且密码匹配
                if (roomNumMap(roomId) >= maxUserNum) {
                  //房间已满
                  userActor ! UserActor.JoinRoomFailure(roomId, 100001, s"room $roomId has been full!")
                } else {
                  //房间未满
                  timer.cancel(RoomEmptyTimerKey(roomId))
                  if(isNewJoin) {
                    roomNumMap.update(roomId, roomNumMap(roomId) + 1)
                  }
                  userRoomMap.put(playerId, (roomId, playerName))
                  userActor ! UserActor.JoinRoomSuccess(roomId, getRoomActor(ctx, roomId))
                }
              } else {
                //房间不存在
                userActor ! UserActor.JoinRoomFailure(roomId, 100002, s"room$roomId  doesn't exist!")
              }
            }
            Behaviors.same

          case CreateRoom(playerId, playerName, userActor,password) =>
            val newRoomId = idGenerator.getAndIncrement()
            log.info(s"room full ,start a new room.. ")
            roomNumMap.put(newRoomId, 1)
            userRoomMap.put(playerId, (newRoomId, playerName))
            userRoomPwdMap.put(newRoomId,(playerId,password.getOrElse("")))
            userActor ! UserActor.JoinRoomSuccess(newRoomId, getRoomActor(ctx, newRoomId))
            Behavior.same


          case UserLeftRoom(playerId, roomId) =>
            if(userRoomMap.get(playerId).nonEmpty){
              if(roomNumMap.get(roomId).nonEmpty) {
                if (roomNumMap(roomId) - 1 <= 0) {
                  roomNumMap.update(roomId, 0)
                  timer.startSingleTimer(RoomEmptyTimerKey(roomId), RoomEmptyKill(roomId), UserLeftRoomTime)
                } else {
                  roomNumMap.update(roomId, roomNumMap(roomId) - 1)
                }
              }
              userRoomMap.remove(playerId)
            }
            Behaviors.same

          case RoomEmptyKill(roomId)=>
            getRoomActor(ctx,roomId) ! RoomActor.KillRoom
            Behaviors.same

          case ChildDead(roomId, childRef) =>
            log.info(s"Child${childRef.path}----$roomId is dead")
            roomNumMap.remove(roomId)
            ctx.unwatch(childRef)
            Behaviors.same

          case GetRoomIdByPlayerId(playerId,sender) =>
            val roomId = userRoomMap.getOrElse(playerId,(-1l, "unknown"))._1
            sender ! RoomIdRsp(roomId)
            Behaviors.same

          case GetRoomListReq(sender) =>
            val roomList = roomNumMap.keys.toList
            sender ! GetRoomListRsp(roomList)
            Behaviors.same

          case GetPlayerListReq(roomId, sender) =>
            val tmpPlayerList = ListBuffer[PlayerInfo]()
            userRoomMap.keys.foreach{ key =>
              if(userRoomMap(key)._1 == roomId){
                tmpPlayerList.append(PlayerInfo(key, userRoomMap(key)._2))
              }
            }
            sender ! GetPlayerListRsp(tmpPlayerList.toList)
            Behaviors.same
            
          case t:GetPlayerByRoomId =>
            val playerId = {
              if (t.playerId == "") {
                val tmpPlayerList = ListBuffer[String]()
                userRoomMap.keys.foreach {
                  key =>
                    if (userRoomMap(key)._1 == t.roomId)
                      tmpPlayerList.append(key)
                }
                if (tmpPlayerList.length <= 0) {
                  ""
                } else {
                  val a = tmpPlayerList(scala.util.Random.nextInt(tmpPlayerList.length))
                  a
                }
              } else {
                t.playerId
              }
            }
            watchManager ! WatcherManager.GetPlayerWatchedRsp(t.watcherId, playerId)
            if(playerId.trim != "") {
              getRoomActor(ctx, t.roomId) ! RoomActor.YourUserIsWatched(playerId, t.watcherRef, t.watcherId)
            }
            Behaviors.same

          case t: YourUserUnwatched =>
            getRoomActor(ctx, t.roomId) ! RoomActor.YouAreUnwatched(t.playerId,t.watcherId)
            Behaviors.same

          case t:INeedApple =>
            getRoomActor(ctx,t.roomId) ! RoomActor.GiveYouApple(t.playerId,t.watcherId)
            Behavior.same

          case x =>
            log.error(s"${ctx.self.path} receive an unknown msg when idle:$x")
            Behaviors.unhandled
        }
    }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Long): ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.create(roomId), childName)
      ctx.watchWith(actor, ChildDead(roomId, actor))
      actor
    }.upcast[RoomActor.Command]
  }

}
