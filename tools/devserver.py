"""Dev server for the Khadmati landing page: mimics the production rewrite
(``/r/* -> index.html``) so room QR links can be tested from the emulator.

Usage:  python tools/devserver.py [port]   (default 8123)
"""
import http.server
import os
import pathlib
import sys

LANDING = pathlib.Path(__file__).resolve().parent.parent / "landing"


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(LANDING), **kwargs)

    def do_GET(self):  # noqa: N802 (stdlib naming)
        if self.path.startswith("/r/"):
            self.path = "/index.html"
        super().do_GET()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8123
    http.server.HTTPServer(("0.0.0.0", port), Handler).serve_forever()
