package simutations

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.http.Predef._
import io.gatling.http.action.ws.WsSendTextFrameBuilder


class Scenario1 extends Simulation {






	val users = ssv("data/users.csv").circular

	val httpProtocol = http
		.baseUrl("https://test.sage-group.info")
		.wsBaseUrl("wss://test.sage-group.info")
		.wsReconnect
		.wsMaxReconnects(100000)
		.acceptHeader("application/json")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("uk-UA,uk;q=0.9,en-US;q=0.8,en;q=0.7,ru;q=0.6")

	val scn = scenario("Scenario1")
		.feed(users)
		.exec(
			http("auth")
			.post("/core/api/Users/login")
			.formParam("username","${username}")
			.formParam("password","${password}")
			.check(status is 200, jsonPath("$.id").saveAs("token"))
				.check(jsonPath("$.userId").saveAs("userId"))
			)
		.exec(
			ws("Set connection").connect("/socket/?EIO=3&transport=websocket")
				.await(30 seconds)( ws.checkTextMessage("connection is set")
					.check(regex(".*sid*")))
		.onConnected(
			exec(
			ws("authentication")
				.sendText("""42["authentication",{"token":"${token}","userId":"${userId}"}]""")
				.await(30 seconds)( ws.checkTextMessage("authenticated")
					.matching(regex("authenticated")))
			)
			.pause(1)
			.exec(
			ws("fetch myself")
				.sendText("""420["/fetchMyself/User"]""")
				.await(30 seconds)( ws.checkTextMessage("userParameter")
					.matching(regex("^430"))
					.check(regex(""""id":"${userId}"""")))
			)
			.pause(1)
			.exec(
			ws("New Session")
				.sendText("""421["/create/Session",{}]""")
				.await(30 seconds)( ws.checkTextMessage("checkSessionId")
					.matching(regex("^431"))
					.check(regex("""id":"(.*)","createdAt"""").saveAs("sessionId")))
			)
			.pause(1)
			.exec(
			ws("Join New Session")
				.sendText("""422["/join/Session",{"id":"${sessionId}"}]""")
				.await(30 seconds)( ws.checkTextMessage("welcome message")
					.matching(regex(""""ai.qubo.parameter.ui.usertext"""")))
			)
			.pause(2)
			.exec(
			ws("enter first number")
				.sendText("""424["/answer/Session", {"id":"${sessionId}","message":"5"}]""")
				.await(100 seconds)( ws.checkTextMessage("second number")
					.matching(regex(""""ai.test.sage.test.case.fact.number2"""")))
			)
			.pause(2)
			.exec(
			ws("enter second number")
				.sendText("""425["/answer/Session", {"id":"${sessionId}","message":"3"}]""")
				.await(100 seconds)( ws.checkTextMessage("operation")
					.matching(regex(""""ai.test.sage.test.case.fact.operation"""")))
			)
			.pause(2)
			.exec(
			ws("select operation")
				.sendText("""426["/answer/Session", {"id":"${sessionId}","message":"Multiplication"}]""")
				.await(100 seconds)( ws.checkTextMessage("result")
					.matching(regex(""""className":"UIServerText""""))
					.check(regex(""""data":"15"""")))
				.await(100 seconds)( ws.checkTextMessage("suggestion to repeat")
					.matching(regex(""""ai.test.sage.test.case.fact.askEndless"""")))
			)
			.pause(2)
			.exec(
			ws("select not to repeat")
				.sendText("""427["/answer/Session", {"id":"${sessionId}","message":"no"}]""")
				.await(100 seconds)( ws.checkTextMessage("thank you")
					.matching(regex(""""className":"UIServerText""""))
					.check(regex(""""data":"Спасибо за обращение"""")))
				.await(100 seconds)( ws.checkTextMessage("select endless")
					.matching(regex(""""ai.qubo.parameter.ui.problemsolved"""")))
			)
			.pause(2)
			.exec(
			ws("select end")
				.sendText("""426["/answer/Session", {"id":"${sessionId}","message":"Да"}]""")
				.await(100 seconds)( ws.checkTextMessage("ending session")
					.matching(regex(""""className":"UIServerText""""))
					.check(regex(""""data":"Завершаем сессию"""")))
			)
		.exec(ws("close").close)))

	setUp(scn.inject(atOnceUsers(50))).protocols(httpProtocol)
}