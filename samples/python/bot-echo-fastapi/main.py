import os
import json
import uuid
import time
import sys

from logging import getLogger, StreamHandler, INFO

from requests.structures import CaseInsensitiveDict
from requests.exceptions import RequestException

from fastapi import FastAPI, Request

import lineworks

BASE_API_URL = "https://www.worksapis.com/v1.0"
BASE_AUTH_URL = "https://auth.worksmobile.com/oauth2/v2.0"
SCOPE = "bot"


global_data = {}
RETRY_COUNT_MAX = 5

app = FastAPI()
logger = getLogger(__name__)
handler = StreamHandler(sys.stdout)
handler.setLevel(INFO)
logger.addHandler(handler)
logger.setLevel(INFO)


@app.post("/callback")
async def callback(request: Request):
    body_raw = await request.body()
    body_json = await request.json()
    headers = CaseInsensitiveDict(request.headers)

    logger.info(body_json)
    logger.info(headers)

    header_bot_id = headers.get("x-works-botid", "dummy")
    signature = headers.get("x-works-signature", "dummy")

    # Load parameters
    bot_id = os.environ.get("LW_API_BOT_ID", "dummy")
    bot_secret = os.environ.get("LW_API_BOT_SECRET", "dummy")
    client_id = os.environ.get("LW_API_CLIENT_ID", "dummy")
    client_secret = os.environ.get("LW_API_CLIENT_SECRET", "dummy")
    service_account_id = os.environ.get("LW_API_SERVICE_ACCOUNT", "dummy")
    privatekey = os.environ.get("LW_API_PRIVATEKEY", "dummy")

    # Validation
    signature = headers.get("x-works-signature")
    if header_bot_id != bot_id or not lineworks.validate_request(body_raw, signature, bot_secret):
        logger.warn("Invalid request")
        return

    user_id = body_json["source"]["userId"]
    content = body_json["content"]

    # Create response content
    res_content = {
        "content": content
    }

    if "access_token" not in global_data:
        # Get Access Token
        logger.info("Get access token")
        res = lineworks.get_access_token(client_id,
                                         client_secret,
                                         service_account_id,
                                         privatekey,
                                         SCOPE)
        global_data["access_token"] = res["access_token"]

    logger.info("reply")
    logger.info(res_content)
    for i in range(RETRY_COUNT_MAX):
        try:
            # Reply message
            res = lineworks.send_message_to_user(res_content,
                                                 bot_id,
                                                 user_id,
                                                 global_data["access_token"])
        except RequestException as e:
            body = e.response.json()
            status_code = e.response.status_code
            if status_code == 403:
                if body["code"] == "UNAUTHORIZED":
                    # Access Token has been expired.
                    # Update Access Token
                    logger.info("Update access token")
                    res = lineworks.get_access_token(client_id,
                                                     client_secret,
                                                     service_account_id,
                                                     privatekey,
                                                     SCOPE)
                    global_data["access_token"] = res["access_token"]
                else:
                    logger.exception(e)
                    break
            elif status_code == 429:
                # Requests over rate limit.
                logger.info("Over rate limit")
                logger.info(body)
            else:
                logger.exception(e)
                break

            # wait and retry
            time.sleep(2 ** i)
        else:
            break

    return {}
