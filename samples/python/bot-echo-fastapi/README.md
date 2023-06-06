# Echo bot sample - Python FastAPI
Sample code for LINE WORKS API bot

## Description
Sample code of LINE WORKS using [FastAPI](https://fastapi.tiangolo.com/).

## Getting Started
### Prepare parameters
Create App and generate parameters on LINE WORKS Developer Console.
https://dev.worksmobile.com/jp/reference/authorization-sa?lang=ja

### Set env

```sh
export LW_API_BOT_ID=1111111
export LW_API_BOT_SECRET=xxxxxxxxxx
export LW_API_CLIENT_ID=xxxxxxxx
export LW_API_CLIENT_SECRET=xxxxxx
export LW_API_SERVICE_ACCOUNT=xxxxx@xxxxxxx
export LW_API_PRIVATEKEY="-----BEGIN PRIVATE KEY-----
xxxxxxxxxxxxxxxxxxxxxxxx
-----END PRIVATE KEY-----"
```

### Run

```sh
pip install -r requirements.txt
uvicorn main:app --reload
```
