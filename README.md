# K-Untis

Kotlin Untis is a partial port for the WebUntis API library from ...somewhere.

## Workflow

1. Enter the user's private data from their QR code from webuntis
2. Send a post req with said data which will generate a JSession token
3. Use that session token to then generate an access token
4. Use the access token to request data from the server

## Playground

I included an HTTPIE config json called `httpie-collection-eduvos.json`. Import that, and you will see the general layout of how the requests are structured.