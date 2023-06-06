import hashlib
import hmac
from base64 import b64encode, b64decode

import jwt
from datetime import datetime
import urllib

import json
import requests

BASE_API_URL = "https://www.worksapis.com/v1.0"
BASE_AUTH_URL = "https://auth.worksmobile.com/oauth2/v2.0"


def validate_request(body: bytes, signature: str, bot_secret: str) -> bool:
    """Validate request

    :param body: request body
    :param signature: value of X-WORKS-Signature header
    :param bot_secret: Bot Secret
    :return: is valid
    """
    secretKey = bot_secret.encode()
    payload = body

    # Encode by HMAC-SHA256 algorithm
    encoded_body = hmac.new(secretKey, payload, hashlib.sha256).digest()
    # BASE64 encode
    encoded_b64_body = b64encode(encoded_body).decode()

    # Compare
    return encoded_b64_body == signature


def __get_jwt(client_id: str, service_account: str, privatekey: str) -> str:
    """Generate JWT for access token

    :param client_id: Client ID
    :param service_account: Service Account
    :param privatekey: Private Key
    :return: JWT
    """
    current_time = datetime.now().timestamp()
    iss = client_id
    sub = service_account
    iat = current_time
    exp = current_time + (60 * 60) # 1 hour

    jws = jwt.encode(
        {
            "iss": iss,
            "sub": sub,
            "iat": iat,
            "exp": exp
        }, privatekey, algorithm="RS256")

    return jws


def get_access_token(client_id: str, client_secret: str, service_account: str, privatekey: str, scope: str) -> dict:
    """Get Access Token

    :param client_id: Client ID
    :param client_secret: Client ID
    :param service_account: Service Account
    :param privatekey: Private Key
    :param scope: OAuth Scope
    :return: response
    """
    # Get JWT
    jwt = __get_jwt(client_id, service_account, privatekey)

    # Get Access Token
    url = '{}/token'.format(BASE_AUTH_URL)

    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }

    params = {
        "assertion": jwt,
        "grant_type": urllib.parse.quote("urn:ietf:params:oauth:grant-type:jwt-bearer"),
        "client_id": client_id,
        "client_secret": client_secret,
        "scope": scope,
    }

    form_data = params

    r = requests.post(url=url, data=form_data, headers=headers)

    body = json.loads(r.text)

    return body


def send_message_to_user(content: dict, bot_id: str, user_id: str, access_token: str):
    """Send message to a user

    :param content: Message content
    :param bot_id: Bot ID
    :param user_id: User ID
    :param access_token: Access Token
    """
    url = "{}/bots/{}/users/{}/messages".format(BASE_API_URL, bot_id, user_id)

    headers = {
          'Content-Type' : 'application/json',
          'Authorization' : "Bearer {}".format(access_token)
        }

    params = content
    form_data = json.dumps(params)

    r = requests.post(url=url, data=form_data, headers=headers)

    r.raise_for_status()
