package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Base64;
import za.ac.sun.cs.green.expr.Expression;

/**
 * Client for communicating with the GreenServer over sockets.
 * Uses Base64-encoded serialized Expression objects for constraint queries.
 *
 * The server runs in a separate non-instrumented JVM to avoid conflicts
 * between Galette bytecode instrumentation and Green/Z3 internals.
 */
public class GreenSocketClient {

    private static final int DEFAULT_PORT = 9408;
    private static final String DEFAULT_HOST = "localhost";
    private static final boolean DEBUG = Boolean.getBoolean("galette.concolic.interception.debug");

    private Socket socket;
    private PrintStream output;
    private BufferedReader input;
    private boolean connected;

    /**
     * Connect to the GreenServer on the default port.
     * @return true if connection successful
     */
    public boolean connect() {
        return connect(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Connect to the GreenServer.
     * @param host the server host
     * @param port the server port
     * @return true if connection successful
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            output = new PrintStream(socket.getOutputStream());
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            if (DEBUG) {
                System.out.println("[GreenSocketClient] Connected to server at " + host + ":" + port);
            }
            return true;
        } catch (IOException e) {
            if (DEBUG) {
                System.err.println(
                        "[GreenSocketClient] Failed to connect to " + host + ":" + port + ": " + e.getMessage());
            }
            connected = false;
            return false;
        }
    }

    /**
     * Check if a constraint is satisfiable.
     * @param expression the constraint expression
     * @return Boolean.TRUE if satisfiable, Boolean.FALSE if unsatisfiable, null on error
     */
    public Boolean isSatisfiable(Expression expression) {
        if (!connected) {
            if (DEBUG) {
                System.err.println("[GreenSocketClient] Not connected to server");
            }
            return null;
        }

        try {
            // Serialize the expression to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(expression);
            oos.close();

            String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());

            if (DEBUG) {
                System.out.println("[GreenSocketClient] Sending constraint: " + expression);
                System.out.println("[GreenSocketClient] Encoded length: " + encoded.length());
            }

            // Send the encoded expression
            output.println(encoded);
            output.flush();

            // Read the response - server sends single char: '1' for SAT, '0' for UNSAT, 'E' for error
            int response = input.read();

            if (DEBUG) {
                System.out.println("[GreenSocketClient] Server response: " + (char) response);
            }

            if (response == '1') {
                return Boolean.TRUE;
            } else if (response == '0') {
                return Boolean.FALSE;
            } else {
                // Error or unexpected response
                return null;
            }
        } catch (IOException e) {
            if (DEBUG) {
                System.err.println("[GreenSocketClient] Error communicating with server: " + e.getMessage());
            }
            connected = false;
            return null;
        }
    }

    /**
     * Close the connection to the server.
     */
    public void close() {
        if (!connected) {
            return;
        }

        try {
            output.println("CLOSE");
            output.flush();
            // Wait for OK response
            input.readLine();
        } catch (IOException e) {
            // Ignore errors during close
        } finally {
            cleanup();
        }
    }

    /**
     * Shutdown the server (use with caution).
     */
    public void shutdown() {
        if (!connected) {
            return;
        }

        try {
            output.println("QUIT");
            output.flush();
        } catch (Exception e) {
            // Ignore errors during shutdown
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        connected = false;
        try {
            if (output != null) output.close();
        } catch (Exception e) {
        }
        try {
            if (input != null) input.close();
        } catch (Exception e) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
        }
    }

    /**
     * Check if currently connected to server.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Static utility method to check satisfiability using a new connection.
     * This is a convenience method for one-off queries.
     *
     * @param expression the constraint to check
     * @return Boolean.TRUE if satisfiable, Boolean.FALSE if unsatisfiable, null on error
     */
    public static Boolean checkSatisfiable(Expression expression) {
        GreenSocketClient client = new GreenSocketClient();
        if (!client.connect()) {
            return null;
        }
        try {
            return client.isSatisfiable(expression);
        } finally {
            client.close();
        }
    }
}
