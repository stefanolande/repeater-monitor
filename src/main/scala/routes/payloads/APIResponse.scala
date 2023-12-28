package routes.payloads

import clients.monitor.Responses.Response
import cats.implicits.*
import clients.monitor.Outcome

object APIResponse {
  extension (s: Outcome) {
    def toResponse: APIResponse = s match
      case Outcome.ACK(response) => APIResponse(s, response.some)
      case _                     => APIResponse(s, None)
  }
}

case class APIResponse(status: Outcome, response: Option[Response] = None)
