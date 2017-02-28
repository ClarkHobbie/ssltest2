import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Created by Clark on 2/27/2017.
 */
public class SSLTest {
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
                    out.flush();
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
                    out.flush();
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

    public static class CommandLine {
        private String[] argv;
        private int argIndex = 0;
        private String mode = "server";
        private boolean useTls = true;
        private String host = "localhost";
        private int port = 6789;

        public String[] getArgv () {
            return argv;
        }

        public String getArg () {
            if (argIndex >= argv.length)
                return null;

            return argv[argIndex];
        }

        public void advance () {
            argIndex++;
        }

        public String getMode () {
            return mode;
        }

        public void setMode (String mode) {
            this.mode = mode;
        }

        public boolean useTls () {
            return useTls;
        }

        public void setUseTls (boolean useTls) {
            this.useTls = useTls;
        }

        public String getHost () {
            return host;
        }

        public void setHost (String host) {
            this.host = host;
        }

        public int getPort () {
            return port;
        }

        public void setPort (int port) {
            this.port = port;
        }

        public CommandLine (String[] argv) {
            this.argv = argv;
            parse();
        }

        public void parse () {
            if (argv.length < 1)
                return;

            if (null != getArg() && getArg().equalsIgnoreCase("nossl")) {
                setUseTls(false);
                advance();
            }

            if (null != getArg()) {
                setMode(getArg());
                advance();
            }

            if (null != getArg()) {
                setHost(getArg());
                advance();
            }

            if (null != getArg()) {
                int temp = Integer.parseInt(getArg());
                setPort(temp);
                advance();
            }
        }
    }

    private boolean useTls = true;

    public boolean useTls () {
        return useTls;
    }

    public SSLTest (boolean useTls) {
        this.useTls = useTls;
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
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";

            String keyStoreFilename = "serverkeystore";
            String keyStorePassword = "whatever";
            KeyStore keyStore = getKeyStore(keyStoreFilename, keyStorePassword);

            KeyStore trustStore = getKeyStore(trustStoreFilename, trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            ServerSocket serverSocket = null;

            if (useTls()) {
                SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
                serverSocket = serverSocketFactory.createServerSocket();
            } else {
                ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
                serverSocket = serverSocketFactory.createServerSocket();
            }

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

    public static X509Certificate getCertificate (String filename, String password, String alias) {
        KeyStore keyStore = null;
        FileInputStream fileInputStream = null;
        Certificate certificate = null;

        try {
            fileInputStream = new FileInputStream(filename);
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(fileInputStream, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return (X509Certificate) certificate;
    }

    public void client (String host, int port) {
        try {
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";
            String trustStoreAlias = "ca";

            KeyStore keyStore = getKeyStore(trustStoreFilename, trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init (keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init (null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            System.out.println ("connecting to " + host + ":" + port);
            SocketFactory socketFactory = SocketFactory.getDefault();

            if (useTls()) {
                socketFactory = sslContext.getSocketFactory();
            }

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
        CommandLine commandLine = new CommandLine(argv);
        SSLTest sslTest = new SSLTest(commandLine.useTls());

        if (commandLine.getMode().equalsIgnoreCase("server"))
            sslTest.server(commandLine.getPort());
        else if (commandLine.getMode().equalsIgnoreCase("client"))
            sslTest.client(commandLine.getHost(), commandLine.getPort());
        else {
            System.err.println ("unknown mode: " + commandLine.getMode());
        }
    }
}
