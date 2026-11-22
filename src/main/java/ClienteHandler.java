import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClienteHandler implements Runnable {
    private static List<ClienteHandler> clientes = new CopyOnWriteArrayList<>();
    private static OuvinteServidor ouvinte = new OuvinteServidor() {};

    public static void setOuvinte(OuvinteServidor o) {
        ouvinte = o;
    }

    private Socket socket;
    private PrintWriter saida;
    private String apelido = "Anônimo";
    private boolean entrou = false;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);

            String primeira = entrada.readLine();
            if (primeira != null && !primeira.isBlank()) {
                apelido = primeira.trim();
            }

            saida.println("Bem-vindo, " + apelido + "!");
            clientes.add(this);
            entrou = true;
            ouvinte.aoEntrar(apelido);
            enviarParaTodos("*** " + apelido + " entrou no chat ***");

            String mensagem;
            while ((mensagem = entrada.readLine()) != null) {
                ouvinte.aoMensagem(apelido, mensagem);
                enviarParaTodos(apelido + ": " + mensagem);
            }
        } catch (IOException e) {
            // cliente caiu
        } finally {
            clientes.remove(this);
            if (entrou) {
                ouvinte.aoSair(apelido);
                enviarParaTodos("*** " + apelido + " saiu do chat ***");
            }
            try { socket.close(); } catch (IOException e) { }
        }
    }

    private void enviarParaTodos(String mensagem) {
        for (ClienteHandler c : clientes) {
            if (c != this) {
                c.saida.println(mensagem);
            }
        }
    }
}