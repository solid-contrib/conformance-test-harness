#!/bin/bash

# This uses the test harness docker image with the default tests pulled from a repository.
# Environment variables are defined in the file `env` in the directory from which you run this script.

mkdir -p reports/css config

# Create the configuration file needed to run CSS in https mode
cat > ./config/css-config.json <<EOF
{
  "@context": "https://linkedsoftwaredependencies.org/bundles/npm/@solid/community-server/^1.0.0/components/context.jsonld",
  "import": [
    "files-scs:config/app/app/default.json",
    "files-scs:config/app/init/default.json",
    "files-scs:config/http/handler/default.json",
    "files-scs:config/http/middleware/websockets.json",
    "files-scs:config/http/static/default.json",
    "files-scs:config/identity/email/default.json",
    "files-scs:config/identity/handler/default.json",
    "files-scs:config/identity/ownership/token.json",
    "files-scs:config/identity/pod/static.json",
    "files-scs:config/ldp/authentication/dpop-bearer.json",
    "files-scs:config/ldp/authorization/webacl.json",
    "files-scs:config/ldp/handler/default.json",
    "files-scs:config/ldp/metadata-parser/default.json",
    "files-scs:config/ldp/metadata-writer/default.json",
    "files-scs:config/ldp/permissions/acl.json",
    "files-scs:config/storage/key-value/memory.json",
    "files-scs:config/storage/resource-store/file.json",
    "files-scs:config/util/auxiliary/acl.json",
    "files-scs:config/util/identifiers/suffix.json",
    "files-scs:config/util/index/default.json",
    "files-scs:config/util/logging/winston.json",
    "files-scs:config/util/representation-conversion/default.json",
    "files-scs:config/util/resource-locker/memory.json",
    "files-scs:config/util/variables/default.json"
  ],
  "@graph": [
    {
      "comment": [
        "An example of what a config could look like if HTTPS is required.",
        "The http/server-factory import above has been omitted since that feature is set below."
      ]
    },
    {
      "comment": "The key/cert values should be replaces with paths to the correct files. The 'options' block can be removed if not needed.",
      "@id": "urn:solid-server:default:ServerFactory",
      "@type": "WebSocketServerFactory",
      "baseServerFactory": {
        "@id": "urn:solid-server:default:HttpServerFactory",
        "@type": "BaseHttpServerFactory",
        "handler": { "@id": "urn:solid-server:default:HttpHandler" },
        "options_showStackTrace": { "@id": "urn:solid-server:default:variable:showStackTrace" },
        "options_https": true,
        "options_key": "/config/server.key",
        "options_cert": "/config/server.cert"
      },
      "webSocketHandler": {
        "@type": "UnsecureWebSocketsProtocol",
        "source": { "@id": "urn:solid-server:default:ResourceStore" }
      }
    }
  ]
}
EOF

# Create a self-signed certificate
openssl req -new -x509 -days 365 -nodes \
  -out config/server.cert \
  -keyout config/server.key \
  -subj "/C=US/ST=California/L=Los Angeles/O=Security/OU=IT Department/CN=server"

# run CSS in a container enabling self-signed certificates
docker run -d --name=server --network=testnet --env NODE_TLS_REJECT_UNAUTHORIZED=0 \
  -v "$(pwd)"/config:/config -p 443:443 -it css:latest \
  -c /config/css-config.json --port=443 --baseUrl=https://server/
# Wait for it to be ready
until $(curl --output /dev/null --silent --head --fail -k https://server); do
  printf '.'
  sleep 1
done
echo 'CSS is running'

# Run the tests in the test harness
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm \
  -v "$(pwd)"/reports/css:/reports \
  --env-file=css.env --network=testnet solidconformancetestbeta/conformance-test-harness \
  --output=/reports --target=css
docker stop server
docker rm server
