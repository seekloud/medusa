package com.neo.sk.medusa.scene

import java.io.ByteArrayInputStream

import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.{Group, Scene}
import javafx.scene.control.{Button, TextField}
import javafx.scene.layout.{GridPane, Pane}
import javafx.scene.paint.{Color, Paint}
import akka.actor.typed.ActorRef
import akka.japi.Effect
import com.neo.sk.medusa.ClientBoot
import com.neo.sk.medusa.actor.WSClient
import com.neo.sk.medusa.common.StageContext
import com.neo.sk.medusa.controller.GameController
import javafx.scene.image.Image
import com.neo.sk.medusa.controller.LoginController
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import javafx.scene.effect.{BoxBlur, DropShadow}
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.FontWeight
import javafx.scene.text.FontPosture

/**
	* Created by wangxicheng on 2018/10/24.
	*/
object LoginScene {
	trait LoginSceneListener {
		def onButtonConnect(email:String, pw:String)
		def onButtonJoin()
		def onButtonBotJoin()
	}
}
class LoginScene() {

	import LoginScene._
	
	val width = 500
	val height = 500
	val group = new Group
	val emailInput = new TextField("email")
	val pwInput = new TextField("password")
	val warningText = new Text("")

	val qrLoginButton = new Button("二维码登录")
	val emailLoginButton = new Button("邮箱登录")
	val joinButton = new Button("Join")
	val botJoinButton = new Button("BotJoin")

	val canvas = new Canvas(width, height)
	val canvasCtx = canvas.getGraphicsContext2D
	var loginSceneListener: LoginSceneListener = _
	
	emailInput.setLayoutX(170)
	emailInput.setLayoutY(150)

	pwInput.setLayoutX(170)
	pwInput.setLayoutY(190)

	warningText.setLayoutX(170)
	warningText.setLayoutY(210)

	qrLoginButton.setLayoutX(180)
	qrLoginButton.setLayoutY(240)
	qrLoginButton.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); " +
												 "-fx-font:17 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic")

	emailLoginButton.setLayoutX(280)
	emailLoginButton.setLayoutY(240)
	emailLoginButton.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); " +
												 "-fx-font:17 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic")
	
	joinButton.setLayoutX(240)
	joinButton.setLayoutY(240)
	joinButton.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:17 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic")
	
	botJoinButton.setLayoutX(260)
	botJoinButton.setLayoutY(240)
	botJoinButton.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:17 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic")
	
	
	
	//canvasCtx.setFill(Color.rgb(153, 255, 153))
	val bgColor = new Color(0.003, 0.176, 0.176, 1.0)
	canvasCtx.setFill(bgColor)
	canvasCtx.fillRect(0, 0, width, height)
	canvasCtx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,28))
	canvasCtx.setFill(Color.web("rgb(250, 250, 250)"))
	canvasCtx.fillText(s"Welcome to medusa!",100,80)
	group.getChildren.add(canvas)
	group.getChildren.add(emailInput)
	group.getChildren.add(pwInput)
	group.getChildren.add(qrLoginButton)


	val scene = new Scene(group)
	
	qrLoginButton.setOnAction { _ =>
		val email = emailInput.getText()
		val pw = pwInput.getText()
		if (email == "") {
			warningText.setText("email不能为空")
		} else if (pw == "") {
			warningText.setText("password不能为空")
		} else {
			loginSceneListener.onButtonConnect(email, pw)
		}
	}

	joinButton.setOnAction(_ => loginSceneListener.onButtonJoin())
	botJoinButton.setOnAction(_ => loginSceneListener.onButtonBotJoin())
	
	def drawScanUrl(imageStream:ByteArrayInputStream) = {
		ClientBoot.addToPlatform{
			group.getChildren.remove(qrLoginButton)
			val img = new Image(imageStream)
			canvasCtx.drawImage(img, 100, 100)
			canvasCtx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,28))
			canvasCtx.fillText(s"请扫码登录！", 160, 70)
		}
	}
	
	def readyToJoin = {
		ClientBoot.addToPlatform {
			canvasCtx.setFill(bgColor)
			canvasCtx.fillRect(0, 0, width, height)
			canvasCtx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,28))
			canvasCtx.setFill(Color.web("rgb(250, 250, 250)"))
			canvasCtx.fillText(s"Welcome to medusa!",100,125)
			group.getChildren.add(joinButton)
			group.getChildren.add(botJoinButton)
		}
	}
	
	def setLoginSceneListener(listener: LoginSceneListener) {
		loginSceneListener = listener
	}
}
