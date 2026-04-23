/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
/**
 *
 * @author Agustin
 */


public class Cliente {

    public static void main(String[] args) {
        String host = "127.0.0.1"; 
        int puerto = 5000;         

        try (Socket socket = new Socket(host, puerto)) {
            System.out.println("Conectado al Servidor");

            // Flujos de entrada y salida para la comunicación
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            
            String mensajeServidor = in.readLine();
            System.out.println("Servidor dice: " + mensajeServidor);

            String mensajeUsuario;

            
            while (true) {
                System.out.print("\nTú: ");
                mensajeUsuario = scanner.nextLine();

            
                out.println(mensajeUsuario);

                // Esperar y leer la respuesta del servidor
                mensajeServidor = in.readLine();
                
                // Si el servidor cerró la conexión o respondió nulo, salimos
                if (mensajeServidor == null) {
                    break;
                }
                
                System.out.println(mensajeServidor);

                
                if (mensajeUsuario.equalsIgnoreCase("SALIR")) {
                    break;
                }
            }

            scanner.close();
            System.out.println("Comunicación finalizada");

        } catch (Exception e) {
            System.out.println("No se pudo conectar al servidor.");
            System.out.println("Error: " + e.getMessage());
        }
    }
}
