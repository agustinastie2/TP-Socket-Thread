package com.mycompany.socketthread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        String host = "127.0.0.1"; 
        int puerto = 5000;         

        try (Socket socket = new Socket(host, puerto)) {
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // 1. HILO DE LECTURA (Escucha al servidor en segundo plano)
            // Esto permite recibir mensajes de otros mientras estás escribiendo.
            Thread hiloLectura = new Thread(() -> {
                try {
                    String mensajeServidor;
                    while ((mensajeServidor = in.readLine()) != null) {
                        // Borra la línea de entrada actual de la consola para que el mensaje no se mezcle con lo que el usuario está escribiendo
                        System.out.print("\r" + mensajeServidor + "\n> "); 
                    }
                } catch (Exception e) {
                    System.out.println("\nDesconectado del servidor.");
                }
            });
            hiloLectura.start();

            // 2. HILO PRINCIPAL DE ESCRITURA (Envía mensajes al servidor)
            String mensajeUsuario;
            while (true) {
                System.out.print("> ");
                mensajeUsuario = scanner.nextLine();

                if (mensajeUsuario != null && !mensajeUsuario.trim().isEmpty()) {
                    out.println(mensajeUsuario);

                    if (mensajeUsuario.equalsIgnoreCase("/SALIR")) {
                        break;
                    }
                }
            }

            scanner.close();
            // Cerramos el socket, lo que provocará que el hiloLectura termine por excepción.
            socket.close(); 
            System.out.println("Aplicación finalizada.");

        } catch (Exception e) {
            System.out.println("No se pudo conectar al servidor.");
            System.out.println("Error: " + e.getMessage());
        }
    }
}