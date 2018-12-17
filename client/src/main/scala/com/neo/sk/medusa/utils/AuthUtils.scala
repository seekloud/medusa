package com.neo.sk.medusa.utils

import com.neo.sk.medusa.common.AppSettings.botSecure
import com.neo.sk.medusa.common.AppSettings._
import com.neo.sk.medusa.protocol.AuthProtocol._
import com.neo.sk.medusa.ClientBoot.executor
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
/**
  *
  * User: yuwei
  * Date: 2018/12/6
  * Time: 10:59
  */
object AuthUtils extends HttpUtil {

  def checkBotToken(apiToken: String) = {
    if(apiToken == botSecure) true
    else false
  }

  def getInfoByEmail(email:String, passwd:String)={
    val methodName = "POST"
    val data = LoginReq(email, passwd).asJson.noSpaces
    val url  = esheepProtocol + "://" + esheepHost + "/esheep/rambler/login"

    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(jsonStr) =>
        decode[ESheepUserInfoRsp](jsonStr) match {
          case Right(res) =>
            Right(res)
          case Left(le) =>
            Left("decode error: "+le)
        }
      case Left(erStr) =>
        Left("get return error:"+erStr)
    }

  }

}
