package model

import model.MonitorResponseStatus

object CommandResponse {
  extension (s: MonitorResponseStatus) {
    def toResponse: CommandResponse = CommandResponse(s)
  }
}

case class CommandResponse(status: MonitorResponseStatus)
