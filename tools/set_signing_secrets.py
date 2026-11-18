#!/usr/bin/env python3
"""把 muse-email fork 的签名密钥放进仓库 Actions Secrets（值全部从文件读，不写进代码）。"""
import base64
import json
import os
import urllib.request

API = "https://api.github.com"
REPO = "boywhyux/muse-email"
TOKEN = os.environ["GITHUB_TOKEN"]
BASE = "/tmp/muse-key"


def req(method, path, body=None):
    r = urllib.request.Request(
        API + path,
        method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(r, timeout=60) as resp:
        return json.load(resp)


from nacl import encoding as nacl_encoding
from nacl import public as nacl_public

pub = req("GET", f"/repos/{REPO}/actions/secrets/public-key")
key_id = pub["key_id"]
pk = nacl_public.PublicKey(pub["key"], nacl_encoding.Base64Encoder)


def set_secret(name, value):
    sealed = nacl_public.SealedBox(pk).encrypt(value.encode())
    enc = base64.b64encode(sealed).decode()
    req("PUT", f"/repos/{REPO}/actions/secrets/{name}",
        {"encrypted_value": enc, "key_id": key_id})
    print("set:", name)


def read(name):
    with open(os.path.join(BASE, name)) as f:
        return f.read().strip()


set_secret("DM_KEYSTORE_B64", read("keystore.b64"))
set_secret("DM_KEYSTORE_PASSWORD", read("store.pass"))
set_secret("DM_KEY_ALIAS", read("alias.txt"))
set_secret("DM_KEY_PASSWORD", read("store.pass"))
print("all secrets set")
