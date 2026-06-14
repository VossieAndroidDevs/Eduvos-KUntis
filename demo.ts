import * as OTPAuth from "otpauth";
import axios from "axios";

// requires
// "axios": "^1.17.0",
// "otpauth": "^9.5.1",


let totp = new OTPAuth.TOTP({
  algorithm: "SHA1",
  digits: 6,
  period: 30,
  secret: "", // secret here
});

const time = new Date().getTime();
const p = await axios({
  url: "https://eduvos-campus.webuntis.com/WebUntis/jsonrpc_intern.do",
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  data: {
    id: 0,
    jsonrpc: "2.0",

    method: "getUserData2017",

    params: [
      {
        auth: {
          clientTime: time,
          user: "", // email here
          otp: totp.generate(),
        },
      },
    ],
  },
});

let id: string = "";
if (p.headers) {
  p.headers["set-cookie"]![0].split(";").forEach((e) => {
    if (e.startsWith("JSESSIONID=")) {
      id = e;
    }
  });
}

if (id !== "") {
  const p = await axios({
    url: "https://eduvos-campus.webuntis.com/WebUntis/api/token/new",
    method: "GET",
    headers: {
      Cookie: id,
      Accept: "application/json",
    },
  });
  console.log(p.data);
}
