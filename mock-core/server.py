#!/usr/bin/env python3
"""Synthetic core-banking contract test stub; never moves money or accesses real accounts."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json, os
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/health':
            return self.respond(200, {'status':'UP'})
        self.respond(404, {'error':'unknown route'})
    def do_POST(self):
        if self.path != '/sandbox/payments/validate':
            return self.respond(404, {'error':'unknown route'})
        if self.headers.get('X-Demo-Api-Key') != os.getenv('CORE_API_KEY','demo-core-key'):
            return self.respond(401,{'error':'invalid sandbox key'})
        size=int(self.headers.get('Content-Length','0'))
        if not (0<size<=2000): return self.respond(413, {'error':'invalid size'})
        try:
            data=json.loads(self.rfile.read(size))
            if self.headers.get('Idempotency-Key') != data['reference']:
                return self.respond(400, {'error':'reference mismatch'})
            if data['currency']!='USD' or float(data['amount'])<=0:
                return self.respond(422, {'error':'invalid transaction'})
        except (KeyError,ValueError,TypeError):
            return self.respond(400,{'error':'invalid JSON'})
        self.respond(200,{'accepted':True,'provider':'SANDBOX_CORE_HTTP',
                          'detail':'Synthetic reference validated; no transaction submitted'})
    def respond(self,code,payload):
        data=json.dumps(payload).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(data)))
        self.end_headers();self.wfile.write(data)
if __name__=='__main__':
    ThreadingHTTPServer(('0.0.0.0',8090),Handler).serve_forever()
