import os
import json

import jwt
from datetime import datetime
import urllib
import requests

BASE_API_URL = "https://www.worksapis.com/v1.0"
BASE_AUTH_URL = "https://auth.worksmobile.com/oauth2/v2.0"


def get_jwt(client_id, service_account_id, privatekey):
    """アクセストークンのためのJWT取得
    """
    current_time = datetime.now().timestamp()
    iss = client_id
    sub = service_account_id
    iat = current_time
    exp = current_time + (60 * 60) # 1時間

    jws = jwt.encode(
        {
            "iss": iss,
            "sub": sub,
            "iat": iat,
            "exp": exp
        }, privatekey, algorithm="RS256")

    return jws


def get_access_token(client_id, client_secret, scope, jws):
    """アクセストークン取得"""
    url = '{}/token'.format(BASE_AUTH_URL)

    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }

    params = {
        "assertion": jws,
        "grant_type": urllib.parse.quote("urn:ietf:params:oauth:grant-type:jwt-bearer"),
        "client_id": client_id,
        "client_secret": client_secret,
        "scope": scope,
    }

    form_data = params

    r = requests.post(url=url, data=form_data, headers=headers)

    body = json.loads(r.text)

    return body


def refresh_access_token(client_id, client_secret, refresh_token):
    """アクセストークン更新"""
    url = '{}/token'.format(BASE_AUTH_URL)

    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }

    params = {
        "refresh_token": refresh_token,
        "grant_type": "refresh_token",
        "client_id": client_id,
        "client_secret": client_secret,
    }

    form_data = params

    r = requests.post(url=url, data=form_data, headers=headers)

    body = json.loads(r.text)

    return body


def send_message(content, bot_id, user_id, access_token):
    """メッセージ送信"""
    url = "{}/bots/{}/users/{}/messages".format(BASE_API_URL, bot_id, user_id)

    headers = {
          'Content-Type' : 'application/json',
          'Authorization' : "Bearer {}".format(access_token)
        }

    params = content
    form_data = json.dumps(params)

    r = requests.post(url=url, data=form_data, headers=headers)

    r.raise_for_status()


def main():
    client_id = os.environ.get("LW_API_20_CLIENT_ID")
    client_secret = os.environ.get("LW_API_20_CLIENT_SECRET")
    service_account_id = os.environ.get("LW_API_20_SERVICE_ACCOUNT_ID")
    privatekey = os.environ.get("LW_API_20_PRIVATEKEY")
    bot_id = os.environ.get("LW_API_20_BOT_ID")
    user_id = os.environ.get("LW_API_20_USER_ID")

    scope = "bot"

    # JWT生成
    jwttoken = get_jwt(client_id, service_account_id, privatekey)

    # アクセストークン取得
    res = get_access_token(client_id, client_secret, scope, jwttoken)

    access_token = res["access_token"]

    # APIリクエスト (メッセージ送信)
    content = {
        "content": {
            "type": "text",
            "text": "Hello"
        }
    }
    content = {
        "content": {
            "type": "flex",
            "altText": "this is a flexible template",
            "contents": {
                "type": "bubble",
                "body": {
                    "type": "box",
                    "layout": "vertical",
                    "contents": [
                        {
                            "type": "text",
                            "text": "hello"
                        },
                        {
                            "type": "text",
                            "text": "world"
                        }
                    ]
                }
            }
        }
    }
    res = send_message(content, bot_id, user_id, access_token)

    return


if __name__ == "__main__":
    main()
