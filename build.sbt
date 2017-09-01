name := """cambosmart-rest-scala"""
version := "1.0-SNAPSHOT"
lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(swagger)
lazy val swagger = RootProject(uri("https://github.com/CreditCardsCom/swagger-play.git"))
scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls", "-language:postfixOps", "-language:implicitConversions")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
doc in Compile <<= target.map(_ / "none")

scalariformSettings
libraryDependencies += filters
libraryDependencies ++= Seq(
  specs2 % Test,
  cache,
  ws,
  "org.reactivemongo"            %% "play2-reactivemongo"          % "0.12.0",
  "io.swagger"                   %% "swagger-play2"                % "1.5.1",
  "org.webjars"                  % "swagger-ui"                    % "2.1.8-M1",

  "org.slf4j"                    % "slf4j-nop"                     % "1.6.4",
  "joda-time"                    % "joda-time"                     % "2.9.4",
  "org.joda"                     % "joda-convert"                  % "1.8.1",

  "org.scalatest"                %% "scalatest"                    % "3.0.0"                      % "test",
  "com.typesafe.play"            % "play-mailer_2.11"              % "5.0.0",
  "org.mindrot"                  % "jbcrypt"                       % "0.3m",
  "com.twilio.sdk"               % "twilio"                        % "7.4.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "QRGen" at "http://kenglxn.github.com/QRGen/repository"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

