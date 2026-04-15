package com.mycompany.socketthread;

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

    // Diccionario seguro para hilos: Mapea "Nombre de Usuario" -> "Flujo de salida (PrintWriter)"
    private static Map<String, PrintWriter> clientesConectados = new ConcurrentHashMap<>();
    private static int contadorUsuarios = 1; // Para asignar nombres automáticos

    public static void main(String[] args) {
        int puerto = 5000;

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("--- Servidor Iniciado ---");
            System.out.println("Esperando conexión en el puerto " + puerto + "...\n");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("[LOG SERVIDOR] ¡Nuevo socket conectado desde: " + clienteSocket.getInetAddress() + "!");

                // Iniciar hilo para el cliente
                HiloCliente hilo = new HiloCliente(clienteSocket);
                new Thread(hilo).start();
            }

        } catch (Exception e) {
            System.out.println("Error crítico en el servidor: " + e.getMessage());
        }
    }

    // --- CLASE HILO CLIENTE ---
    static class HiloCliente implements Runnable {
        private Socket socket;
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

                // Asignar nombre y registrar al usuario
                synchronized (Servidor.class) {
                    nombreUsuario = "Usuario" + contadorUsuarios++;
                }
                clientesConectados.put(nombreUsuario, out);

                // --- MENÚ DE BIENVENIDA ---
                out.println("=====================================================");
                out.println("¡Bienvenido al servidor! Tu nombre asignado es: " + nombreUsuario);
                out.println("Comandos disponibles:");
                out.println(" /AYUDA             - Muestra este menú");
                out.println(" /FECHA             - Muestra la fecha y hora actual");
                out.println(" /LISTAR            - Lista los usuarios conectados");
                out.println(" /RESOLVE \"expr\"  - Resuelve una ecuación (ej: /RESOLVE \"2+2\")");
                out.println(" /MSG usr1,usr2 msj - Envía mensaje privado a usuarios específicos");
                out.println(" /TODOS msj         - Envía un mensaje a todos los conectados");
                out.println(" /SALIR             - Desconectarse del servidor");
                out.println("=====================================================");

                // Avisar a los demás que alguien entró
                difundirMensaje("Servidor", "¡" + nombreUsuario + " se ha unido al chat!", null);

                String mensajeRecibido;
                while ((mensajeRecibido = in.readLine()) != null) {
                    // Log en consola del servidor (Requisito)
                    System.out.println("[LOG SERVIDOR] " + nombreUsuario + " envió: " + mensajeRecibido);

                    if (mensajeRecibido.trim().isEmpty()) continue;

                    // --- PROCESAMIENTO DE COMANDOS ---
                    if (mensajeRecibido.equalsIgnoreCase("/SALIR")) {
                        out.println("Desconectando... ¡Adiós!");
                        break;
                    } 
                    else if (mensajeRecibido.equalsIgnoreCase("/AYUDA")) {
                        out.println("Comandos: /AYUDA, /FECHA, /LISTAR, /RESOLVE \"expr\", /MSG usr1,usr2 msj, /TODOS msj, /SALIR");
                    } 
                    else if (mensajeRecibido.equalsIgnoreCase("/FECHA")) {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                        out.println("Servidor -> Fecha y hora actual: " + dtf.format(LocalDateTime.now()));
                    } 
                    else if (mensajeRecibido.equalsIgnoreCase("/LISTAR")) {
                        out.println("Servidor -> Usuarios conectados (" + clientesConectados.size() + "): " + String.join(", ", clientesConectados.keySet()));
                    } 
                    else if (mensajeRecibido.toUpperCase().startsWith("/RESOLVE")) {
                        resolverMatematica(mensajeRecibido);
                    } 
                    else if (mensajeRecibido.toUpperCase().startsWith("/TODOS ")) {
                        String msj = mensajeRecibido.substring(7);
                        difundirMensaje(nombreUsuario, msj, null);
                        out.println("Servidor -> Mensaje enviado a todos.");
                    } 
                    else if (mensajeRecibido.toUpperCase().startsWith("/MSG ")) {
                        enviarPrivado(mensajeRecibido);
                    } 
                    else {
                        out.println("Servidor -> Comando no reconocido. Escribe /AYUDA para ver la lista.");
                    }
                }

            } catch (Exception e) {
                System.out.println("[LOG SERVIDOR] Error con " + nombreUsuario + ": " + e.getMessage());
            } finally {
                // Limpieza cuando el cliente se desconecta
                if (nombreUsuario != null) {
                    clientesConectados.remove(nombreUsuario);
                    difundirMensaje("Servidor", nombreUsuario + " se ha desconectado.", null);
                    System.out.println("[LOG SERVIDOR] " + nombreUsuario + " ha salido.");
                }
                try { socket.close(); } catch (Exception e) {}
            }
        }

        // Método para resolver ecuaciones
        private void resolverMatematica(String comando) {
            try {
                int primerComilla = comando.indexOf('"');
                int ultimaComilla = comando.lastIndexOf('"');

                if (primerComilla != -1 && ultimaComilla != -1 && primerComilla < ultimaComilla) {
                    String ecuacion = comando.substring(primerComilla + 1, ultimaComilla);
                    double resultado = evaluarMatematica(ecuacion);
                    out.println("Servidor -> El resultado de [" + ecuacion + "] es: " + resultado);
                } else {
                    out.println("Error: Formato incorrecto. Ejemplo válido: /RESOLVE \"45*2+10\"");
                }
            } catch (Exception e) {
                out.println("Error del Servidor -> No se pudo resolver la ecuación. Verifica la sintaxis.");
            }
        }

        // Método para procesar mensajes a destinatarios específicos
        private void enviarPrivado(String comando) {
            // Esperado: "/MSG user1,user2 Hola como estan"
            String[] partes = comando.split(" ", 3); 
            if (partes.length < 3) {
                out.println("Error: Formato incorrecto. Uso: /MSG usuario1,usuario2 mensaje");
                return;
            }

            String[] destinatarios = partes[1].split(",");
            String mensaje = partes[2];
            List<String> noEncontrados = new ArrayList<>();
            int enviados = 0;

            for (String dest : destinatarios) {
                dest = dest.trim();
                PrintWriter writerDestino = clientesConectados.get(dest);
                
                if (writerDestino != null) {
                    writerDestino.println("[Mensaje Privado de " + nombreUsuario + "]: " + mensaje);
                    enviados++;
                } else {
                    noEncontrados.add(dest);
                }
            }

            // Reporte al emisor
            if (enviados > 0) {
                out.println("Servidor -> Mensaje entregado a " + enviados + " usuario(s).");
            }
            if (!noEncontrados.isEmpty()) {
                out.println("Servidor [ALERTA] -> Los siguientes usuarios no existen o no están conectados: " + String.join(", ", noEncontrados));
            }
        }

        // Envía un mensaje a todos, excluyendo opcionalmente a un usuario
        private void difundirMensaje(String remitente, String mensaje, String excluirUsuario) {
            for (Map.Entry<String, PrintWriter> entrada : clientesConectados.entrySet()) {
                if (excluirUsuario == null || !entrada.getKey().equals(excluirUsuario)) {
                    // No enviarse a sí mismo
                    if(!entrada.getKey().equals(remitente)){
                         entrada.getValue().println("[" + remitente + " a TODOS]: " + mensaje);
                    }
                }
            }
        }

        // Algoritmo matemático
        public double evaluarMatematica(final String str) {
            return new Object() {
                int pos = -1, ch;
                void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }
                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) { nextChar(); return true; }
                    return false;
                }
                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) throw new RuntimeException("Inesperado: " + (char) ch);
                    return x;
                }
                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if (eat('+')) x += parseTerm();
                        else if (eat('-')) x -= parseTerm();
                        else return x;
                    }
                }
                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if (eat('*')) x *= parseFactor();
                        else if (eat('/')) x /= parseFactor();
                        else return x;
                    }
                }
                double parseFactor() {
                    if (eat('+')) return parseFactor();
                    if (eat('-')) return -parseFactor();
                    double x;
                    int startPos = this.pos;
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else {
                        throw new RuntimeException("Inesperado: " + (char) ch);
                    }
                    return x;
                }
            }.parse();
        }
    }
}