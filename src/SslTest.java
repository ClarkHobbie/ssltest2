import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Created by Clark on 2/27/2017.
 */
public class SslTest {
    public static class Server implements Runnable {
        private Socket socket;

        public Server (Socket socket) {
            this.socket = socket;
        }

        public void run () {
            System.out.println ("got connection from:" + socket.getInetAddress());

            BufferedReader in = null;
            PrintWriter out = null;

            try {
                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                in = new BufferedReader(inputStreamReader);

                OutputStream outputStream = socket.getOutputStream();
                out = new PrintWriter(outputStream);

                String s = "";

                while (!s.equalsIgnoreCase("quit") && !s.equalsIgnoreCase("bye")) {
                    s = in.readLine();
                    System.out.println ("got " +s);
                    out.println(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                closeIgnoreExceptions(in);
                closeIfNonNull(out);
            }
        }
    }

    public static class Client implements Runnable {
        private Socket socket;
        private String prompt;

        public Client (Socket socket, String prompt) {
            this.socket = socket;
            this.prompt = prompt;
        }

        public void run () {
            BufferedReader inFromSocket = null;
            BufferedReader inFromUser = null;
            PrintWriter out = null;

            try {
                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                inFromSocket = new BufferedReader (inputStreamReader);

                OutputStream outputStream = socket.getOutputStream();
                out = new PrintWriter(outputStream);

                inputStreamReader = new InputStreamReader(System.in);
                inFromUser = new BufferedReader(inputStreamReader);

                String s = "";

                do {
                    System.out.print(prompt);
                    s = inFromUser.readLine();
                    out.println(s);
                    s = inFromSocket.readLine();
                    System.out.println(s);
                } while (!s.equalsIgnoreCase("quit") && !s.equalsIgnoreCase("bye"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                closeIgnoreExceptions(inFromSocket);
                closeIgnoreExceptions(inFromUser);
                closeIfNonNull(out);
            }
        }
    }

    public static void closeIgnoreExceptions (Reader reader)
    {
        if (null != reader) {
            try {
                reader.close();
            } catch (IOException e) {}
        }
    }

    public static void closeIgnoreExceptions (InputStream inputStream) {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {}
        }
    }

    public static void closeIfNonNull (PrintWriter printWriter) {
        if (null != printWriter) {
            printWriter.close();
        }
    }

    public KeyStore getKeyStore (String filename, String password) {
        KeyStore keyStore = null;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(filename);
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load (fileInputStream, password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            closeIgnoreExceptions(fileInputStream);
        }

        return keyStore;
    }


    public void server (int port) {
        try {
            String truststoreFilename = "truststore";
            String truststorePassword = "whatever";
            KeyStore trustStore = getKeyStore(truststoreFilename, truststorePassword);

            String keyStoreFilename = "serverkeystore";
            String keyStorePassword = "whatever";
            KeyStore keyStore = getKeyStore(keyStoreFilename, keyStorePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            ServerSocket serverSocket = serverSocketFactory.createServerSocket();
            InetSocketAddress socketAddress = new InetSocketAddress(port);
            serverSocket.bind(socketAddress);

            System.out.println ("listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread (new Server(socket));
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void client (String host, int port) {
        try {
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";
            KeyStore keyStore = getKeyStore(trustStoreFilename, trustStorePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init (keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init (null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            SocketFactory socketFactory = sslContext.getSocketFactory();
            System.out.println ("connecting to " + host + ":" + port);
            Socket socket = socketFactory.createSocket(host, port);

            Client client = new Client(socket, host + "> ");
            Thread thread = new Thread(client);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public static void main (String[] argv) {
        String mode = "server";
        String host = "localhost";
        int port = 6789;
        SslTest sslTest = new SslTest();

        if (argv.length > 0)
            mode = argv[0];

        if (argv.length > 1)
            host = argv[1];

        if (argv.length > 2)
            port = Integer.parseInt(argv[2]);


        if (mode.equalsIgnoreCase("server"))
            sslTest.server(port);
        else if (mode.equalsIgnoreCase("client"))
            sslTest.client(host,port);
        else {
            System.err.println ("unknown mode: " + mode);
        }
    }
}
