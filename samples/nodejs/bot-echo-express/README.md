# Echo bot sample - Node.js Express
Sample code for LINE WORKS Bot

## Description
Sample code of LINE WORKS Bot using [Express](https://expressjs.com/).

## Getting Started
### Requirements

- Node.js >= 16

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
npm install
node index.js
```
