package com.neo.sk.medusa.snake

object Protocol4Agent {
  //申请登陆
    case class UrlData(
                      wsUrl:String,
                      scanUrl:String
                      )
    case class LoginResponse(
                            data:UrlData,
                            errCode:Int = 0,
                            msg:String = "ok"
                           )
    case class WsData(
                     userId:Long,
                     nickname:String,
                     token:String,
                     tokenExpireTime:Long
                     )
    case class WsResponse(
                        data:WsData,
                        errCode:Int = 0,
                        msg:String = "ok"
                        )
  case class Ws4AgentResponse(
                               Ws4AgentRsp:WsResponse
                        )
  //连接游戏
    case class LinkGameData(
                           gameId:Long,
                           playerId:String
                           )


    case class GameServerInfo(
                             ip:String,
                             port:Int,
                             domain:String
                             )
    case class LinkResElement(
                               accessCode:String,
                               gsPrimaryInfo:GameServerInfo
                             )
    case class LinkGameRes(
                          data:LinkResElement,
                          errCode:Int = 0,
                          msg:String = "ok"
                          )

}
