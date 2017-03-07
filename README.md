A test for SSL/TLS certificates

This program tests SSL certificates and the like.

This program is intended to test out local certificate authorities.  Use the truststore and
serverkeystore that came with the project or create your own.  The commads to create a new
truststore and keystore are:

`openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes`
`keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever`
`keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever`
`keytool –keystore serverkeystore -storepass whatever –certreq –alias server –file server.csr`
`openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial`
`keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca`
`keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server`

Compile the program with the following command:

           javac src\SSLTest.java

Run the server with the following

           java -cp src SSLTest server

In another window, run the client with the following command

           java -cp src SSLTest client

In a bit more accessible format, the commands to create a truststore and server keystore:

1) Create the local CA self-signed certificate and private key

    openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes

2) Create the truststore

    keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever

3) Create the server keystore

    keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever

4) Create a certificate signing request for the server

    keytool –keystore serverkeystore -storepass whatever –certreq –alias server –file server.csr

5) Sign the server CSR with the local CA

    openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial

6) Import the local CA to the server keystore

    keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca

7) Import the singed certificate to the sever kestore

    keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server

Compile the program with the following command:

    javac src\SSLTest.java

