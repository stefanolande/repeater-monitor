package routes.model

import model.controller.Outcome

object APIResponse {
  extension (s: Outcome) {
    def toResponse: APIResponse = APIResponse(s)
  }
}

case class APIResponse(status: Outcome)
