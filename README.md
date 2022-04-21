# llocer_ocpi

OCPI implementation:
- http client
- http servlet
- OCPI modules

See repository [llocer_ev_examples](https://github.com/Llocer/llocer_ev_examples) for usage examples.

## configuration

Optional configuration file is /etc/ocpi.conf. It must be in json format. The default is:

    {
	    "public_url": "http://127.0.0.1:8080",
	    "private_uri_length": 0,
	    "pagination_limit": 100,
	    "testing_no_change_credentials": false
    }

where:

- public_url: The server public address (not including the trailing path of the OCPI sevrlets)
- private_url_length : The length of the path of the server private address. Usually, if the server is not behind a loadbalancer or similar, the public and private addresses will be the same.
- pagination_limit: maximum number of items to be sent in a pagination request.
- testing_no_change_credentials: for testing only, a new token will be not generated when new credentials are received from a peer node.

Example:

Assume you public server address is "https://my.com/cso". The loadbalancer redirects the request to a private server with address "https://192.168.21.21:8080/foo/bar" and you have an OCPI servlet serving request on it at "https://192.168.21.21:8080/foo/bar/cso/ocpi/...". Configuration must be:

- "public_url": "https://my.com/cso"
- "private_url_length": 4 (number of elements in "/foo/bar/cso/ocpi")
