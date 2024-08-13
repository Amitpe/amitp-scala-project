package testkit

object ResponseFactory {
  val postMessageResponse =
    s"""
     {
         "ok": true,
         "channel": "C97NS35J9",
         "ts": "1586377134.000100",
         "message": {
             "type": "message",
             "subtype": "bot_message",
             "text": "test",
             "ts": "1586377134.000100",
             "username": "bot",
             "bot_id": "B9P4KDZTQ"
         }
     }
     """
}
