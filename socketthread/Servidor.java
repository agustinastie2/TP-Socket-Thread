/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.socketthread;

/**
 *
 * @author Agustin
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final Map<String, PrintWriter> clientesConectados = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("--- Servidor Iniciado ---");
            System.out.println("Esperando conexiones en el puerto " + PUERTO + "...\n");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("[LOG SERVIDOR] Nuevo socket conectado desde: "
                        + clienteSocket.getInetAddress() + ":" + clienteSocket.getPort());

                Thread hilo = new Thread(new HiloCliente(clienteSocket));
                hilo.start();
            }
        } catch (Exception e) {
            System.out.println("[ERROR CRITICO] No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    private static synchronized String generarNombreUnico(String nombreSolicitado) {
        String base = (nombreSolicitado == null) ? "" : nombreSolicitado.trim();
        if (base.isEmpty()) {
            base = "Usuario";
        }

        String nombreFinal = base;
        int sufijo = 2;

        while (clientesConectados.containsKey(nombreFinal)) {
            nombreFinal = base + sufijo;
            sufijo++;
        }

        return nombreFinal;
    }

    static class HiloCliente implements Runnable {
        private final Socket socket;
        private String nombreUsuario;
        private BufferedReader in;
        private PrintWriter out;

        public HiloCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                solicitarYRegistrarNombre();
                enviarMenuBienvenida();
                difundirMensaje("Servidor", "¡" + nombreUsuario + " se ha unido al chat!", nombreUsuario);

                String mensajeRecibido;
                while ((mensajeRecibido = in.readLine()) != null) {
                    mensajeRecibido = mensajeRecibido.trim();

                    if (mensajeRecibido.isEmpty()) {
                        continue;
                    }

                    System.out.println("[LOG SERVIDOR] " + nombreUsuario + " envió: " + mensajeRecibido);

                    if (mensajeRecibido.equalsIgnoreCase("/SALIR")) {
                        out.println("Servidor -> Desconectando... ¡Adiós!");
                        break;
                    } else if (mensajeRecibido.equalsIgnoreCase("/AYUDA")) {
                        enviarAyudaResumida();
                    } else if (mensajeRecibido.equalsIgnoreCase("/FECHA")) {
                        out.println("Servidor -> Fecha y hora actual: " + FORMATO_FECHA.format(LocalDateTime.now()));
                    } else if (mensajeRecibido.equalsIgnoreCase("/LISTAR")) {
                        out.println("Servidor -> Usuarios conectados (" + clientesConectados.size() + "): "
                                + String.join(", ", clientesConectados.keySet()));
                    } else if (mensajeRecibido.toUpperCase().startsWith("/RESOLVE")) {
                        resolverMatematica(mensajeRecibido);
                    } else if (mensajeRecibido.toUpperCase().startsWith("/TODOS ")) {
                        enviarATodos(mensajeRecibido);
                    } else if (mensajeRecibido.toUpperCase().startsWith("/MSG ")) {
                        enviarPrivado(mensajeRecibido);
                    } else {
                        out.println("Servidor -> Comando no reconocido. Escribe /AYUDA para ver la lista de comandos.");
                    }
                }
            } catch (Exception e) {
                System.out.println("[LOG SERVIDOR] Error con " + nombreUsuario + ": " + e.getMessage());
            } finally {
                desconectarCliente();
            }
        }

        private void solicitarYRegistrarNombre() throws Exception {
            out.println("Servidor -> Ingresa tu nombre de usuario:");
            String nombreSolicitado = in.readLine();

            if (nombreSolicitado == null) {
                throw new Exception("El cliente no envió nombre de usuario.");
            }

            nombreSolicitado = normalizarNombre(nombreSolicitado);
            String nombreAsignado = generarNombreUnico(nombreSolicitado);
            clientesConectados.put(nombreAsignado, out);
            nombreUsuario = nombreAsignado;

            if (!nombreUsuario.equals(nombreSolicitado)) {
                out.println("Servidor -> El nombre solicitado ya estaba en uso o no era válido.");
            }
            out.println("Servidor -> Nombre asignado: " + nombreUsuario);
            System.out.println("[LOG SERVIDOR] Usuario registrado como: " + nombreUsuario);
        }

        private String normalizarNombre(String nombre) {
            String limpio = nombre == null ? "" : nombre.trim();
            limpio = limpio.replaceAll("\\s+", "_");
            limpio = limpio.replaceAll("[^a-zA-Z0-9_-]", "");
            return limpio;
        }

        private void enviarMenuBienvenida() {
            out.println("=====================================================");
            out.println("¡Bienvenido al servidor, " + nombreUsuario + "!");
            out.println("Comandos disponibles:");
            out.println(" /AYUDA               - Muestra la ayuda");
            out.println(" /FECHA               - Muestra la fecha y hora actual");
            out.println(" /LISTAR              - Lista los usuarios conectados");
            out.println(" /RESOLVE \"expr\"    - Resuelve una expresión matemática");
            out.println(" /MSG usr1,usr2 msj   - Envía mensaje privado a uno o varios usuarios");
            out.println(" /TODOS msj           - Envía un mensaje a todos los conectados");
            out.println(" /SALIR               - Desconectarse del servidor");
            out.println("=====================================================");
        }

        private void enviarAyudaResumida() {
            out.println("Servidor -> Comandos disponibles: /AYUDA, /FECHA, /LISTAR, /RESOLVE \"expr\", /MSG usr1,usr2 mensaje, /TODOS mensaje, /SALIR");
        }

        private void enviarATodos(String comando) {
            String mensaje = comando.substring(7).trim();
            if (mensaje.isEmpty()) {
                out.println("Servidor -> Debes escribir un mensaje luego de /TODOS.");
                return;
            }

            difundirMensaje(nombreUsuario, mensaje, nombreUsuario);
            out.println("Servidor -> Mensaje enviado a todos los usuarios conectados.");
        }

        private void resolverMatematica(String comando) {
            try {
                int primeraComilla = comando.indexOf('"');
                int ultimaComilla = comando.lastIndexOf('"');

                if (primeraComilla == -1 || ultimaComilla == -1 || primeraComilla >= ultimaComilla) {
                    out.println("Servidor -> Formato incorrecto. Ejemplo: /RESOLVE \"45*2+10\"");
                    return;
                }

                String expresion = comando.substring(primeraComilla + 1, ultimaComilla);
                double resultado = evaluarMatematica(expresion);
                out.println("Servidor -> El resultado de [" + expresion + "] es: " + resultado);
            } catch (Exception e) {
                out.println("Servidor -> No se pudo resolver la expresión. Verifica la sintaxis.");
            }
        }

        private void enviarPrivado(String comando) {
            String[] partes = comando.split(" ", 3);
            if (partes.length < 3) {
                out.println("Servidor -> Formato incorrecto. Uso: /MSG usuario1,usuario2 mensaje");
                return;
            }

            String[] destinatarios = partes[1].split(",");
            String mensaje = partes[2].trim();

            if (mensaje.isEmpty()) {
                out.println("Servidor -> Debes escribir un mensaje luego de los destinatarios.");
                return;
            }

            List<String> noEncontrados = new ArrayList<>();
            List<String> entregados = new ArrayList<>();

            for (String destinatario : destinatarios) {
                String dest = destinatario.trim();
                if (dest.isEmpty()) {
                    continue;
                }

                PrintWriter writerDestino = clientesConectados.get(dest);
                if (writerDestino != null) {
                    writerDestino.println("[Mensaje Privado de " + nombreUsuario + "]: " + mensaje);
                    entregados.add(dest);
                } else {
                    noEncontrados.add(dest);
                }
            }

            if (!entregados.isEmpty()) {
                out.println("Servidor -> Mensaje entregado a: " + String.join(", ", entregados));
            }
            if (!noEncontrados.isEmpty()) {
                out.println("Servidor [ALERTA] -> Los siguientes usuarios no existen o no están conectados: "
                        + String.join(", ", noEncontrados));
            }
            if (entregados.isEmpty() && noEncontrados.isEmpty()) {
                out.println("Servidor -> No se especificaron destinatarios válidos.");
            }
        }

        private void difundirMensaje(String remitente, String mensaje, String excluirUsuario) {
            for (Map.Entry<String, PrintWriter> entrada : clientesConectados.entrySet()) {
                String usuarioDestino = entrada.getKey();
                if (excluirUsuario != null && usuarioDestino.equals(excluirUsuario)) {
                    continue;
                }
                entrada.getValue().println("[" + remitente + " a TODOS]: " + mensaje);
            }
        }

        private void desconectarCliente() {
            try {
                if (nombreUsuario != null) {
                    clientesConectados.remove(nombreUsuario);
                    difundirMensaje("Servidor", nombreUsuario + " se ha desconectado.", null);
                    System.out.println("[LOG SERVIDOR] " + nombreUsuario + " ha salido.");
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("[LOG SERVIDOR] Error al cerrar conexión: " + e.getMessage());
            }
        }

        public double evaluarMatematica(final String str) {
            return new Object() {
                int pos = -1;
                int ch;

                void nextChar() {
                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
                }

                boolean eat(int charToEat) {
                    while (ch == ' ') {
                        nextChar();
                    }
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }

                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) {
                        throw new RuntimeException("Caracter inesperado: " + (char) ch);
                    }
                    return x;
                }

                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if (eat('+')) {
                            x += parseTerm();
                        } else if (eat('-')) {
                            x -= parseTerm();
                        } else {
                            return x;
                        }
                    }
                }

                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if (eat('*')) {
                            x *= parseFactor();
                        } else if (eat('/')) {
                            x /= parseFactor();
                        } else {
                            return x;
                        }
                    }
                }

                double parseFactor() {
                    if (eat('+')) {
                        return parseFactor();
                    }
                    if (eat('-')) {
                        return -parseFactor();
                    }

                    double x;
                    int startPos = this.pos;

                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) {
                            throw new RuntimeException("Falta cerrar paréntesis");
                        }
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') {
                            nextChar();
                        }
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else {
                        throw new RuntimeException("Caracter inesperado: " + (char) ch);
                    }

                    return x;
                }
            }.parse();
        }
    }
}

