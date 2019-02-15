rm -f rootCA.key
rm -f rootCA.pem
rm -f ssl.key
rm -f ssl.csr
rm -f ssl.cert

openssl genrsa -out rootCA.key 2048

openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 1024 -out rootCA.pem

openssl genrsa -out ssl.key 2048

openssl req -new -key ssl.key -out ssl.csr -subj "/CN=quay-enterprise" -config openssl.cnf

openssl x509 -req -in ssl.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out ssl.cert -days 356 -extensions v3_req -extfile openssl.cnf

cp rootCA.pem rootCA.crt
