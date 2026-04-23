/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

        Scanner scanner = new Scanner(System.in);

        System.out.print("Ingresa tu nombre de usuario: ");
        String nombreUsuario = scanner.nextLine();

        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            nombreUsuario = "Usuario";
        }

        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(nombreUsuario);

            Thread hiloLectura = new Thread(() -> {
                try {
                    String mensajeServidor;
                    while ((mensajeServidor = in.readLine()) != null) {
                        System.out.print("\r" + mensajeServidor + "\n> ");
                    }
                } catch (Exception e) {
                    System.out.println("\nDesconectado del servidor.");
                }
            });

            hiloLectura.setDaemon(true);
            hiloLectura.start();

            while (true) {
                System.out.print("> ");
                String mensajeUsuario = scanner.nextLine();

                if (mensajeUsuario == null || mensajeUsuario.trim().isEmpty()) {
                    continue;
                }

                out.println(mensajeUsuario);

                if (mensajeUsuario.equalsIgnoreCase("/SALIR")) {
                    break;
                }
            }

            System.out.println("Aplicación finalizada.");
        } catch (Exception e) {
            System.out.println("No se pudo conectar al servidor.");
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
